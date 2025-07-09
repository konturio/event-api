package io.kontur.eventapi.usgs.earthquake.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.usgs.earthquake.client.UsgsEarthquakeClient;
import io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.util.JsonUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;


@Component
public class UsgsEarthquakeImportJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(UsgsEarthquakeImportJob.class);
    private final UsgsEarthquakeClient client;
    private final DataLakeDao dataLakeDao;
    private final UsgsEarthquakeDataLakeConverter converter;

    public UsgsEarthquakeImportJob(MeterRegistry meterRegistry, UsgsEarthquakeClient client,
                                   DataLakeDao dataLakeDao, UsgsEarthquakeDataLakeConverter converter) {
        super(meterRegistry);
        this.client = client;
        this.dataLakeDao = dataLakeDao;
        this.converter = converter;
    }

    @Override
    public String getName() {
        return "usgsEarthquakeImport";
    }

    @Override
    public void execute() {
        try {
            String geoJson = client.getEarthquakes();
            if (StringUtils.isBlank(geoJson)) {
                LOG.warn("Skip processing usgs earthquake feed due to empty response");
                return;
            }
            JsonNode root = JsonUtil.readTree(geoJson);
            ArrayNode features = (ArrayNode) root.get("features");
            List<DataLake> dataLakes = new ArrayList<>();
            for (JsonNode node : features) {
                try {
                    ObjectNode feature = (ObjectNode) node;
                    String externalId = StringUtils.strip(feature.get("id").asText(), "\"");
                    JsonNode updatedNode = feature.get("properties").get("updated");
                    if (updatedNode != null && StringUtils.isNotBlank(externalId)) {
                        OffsetDateTime updatedAt = DateTimeUtil.getDateTimeFromMilli(updatedNode.asLong());
                        enrichWithShakemap(feature, externalId);
                        dataLakes.add(converter.convert(externalId, updatedAt, feature.toString()));
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process feature from usgs earthquake feed", e);
                }
            }
            if (!dataLakes.isEmpty()) {
                dataLakeDao.storeDataLakes(dataLakes);
            }
        } catch (Exception e) {
            LOG.warn("Error while obtaining usgs earthquake feed", e);
        }
    }

    private void enrichWithShakemap(ObjectNode feature, String externalId) {
        try {
            JsonNode typesNode = feature.get("properties").get("types");
            if (typesNode == null || !typesNode.asText().contains("shakemap")) {
                return;
            }
            String detailJson = client.getDetail(externalId);
            if (StringUtils.isBlank(detailJson)) {
                feature.put("shakemap", false);
                return;
            }
            JsonNode detail = JsonUtil.readTree(detailJson);
            JsonNode shakemapArray = detail.at("/properties/products/shakemap");
            if (!shakemapArray.isArray() || shakemapArray.size() == 0) {
                feature.put("shakemap", false);
                return;
            }
            JsonNode first = shakemapArray.get(0);
            ObjectNode result = (ObjectNode) JsonUtil.readTree("{}");
            copyField(first, result, "indexid");
            copyField(first, result, "indexTime");
            copyField(first, result, "id");
            copyField(first, result, "type");
            copyField(first, result, "code");
            copyField(first, result, "source");
            copyField(first, result, "updateTime");
            copyField(first, result, "status");
            JsonNode props = first.get("properties");
            if (props != null) {
                result.set("properties", props);
            }
            JsonNode contNode = first.at("/contents/download/cont_pga.json");
            if (contNode.isMissingNode()) {
                feature.put("shakemap", false);
                return;
            }
            result.set("download/cont_pga.json", contNode);
            String url = contNode.get("url").asText();
            String contPga = fetchUrl(url);
            if (contPga == null) {
                feature.put("shakemap", false);
                return;
            }
            result.put("cont_pga", contPga);
            ArrayNode arr = feature.putArray("shakemap");
            arr.add(result);
        } catch (Exception e) {
            LOG.warn("Failed to enrich feature with shakemap", e);
            feature.put("shakemap", false);
        }
    }

    private void copyField(JsonNode from, ObjectNode to, String field) {
        JsonNode node = from.get(field);
        if (node != null) {
            to.set(field, node);
        }
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String fetchUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return resp.body();
            }
        } catch (Exception e) {
            LOG.warn("Failed to download url {}", url, e);
        }
        return null;
    }
}
