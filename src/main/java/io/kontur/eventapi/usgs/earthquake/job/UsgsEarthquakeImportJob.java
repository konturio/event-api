package io.kontur.eventapi.usgs.earthquake.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class UsgsEarthquakeImportJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(UsgsEarthquakeImportJob.class);

    private final UsgsEarthquakeClient client;
    private final DataLakeDao dataLakeDao;
    private final UsgsEarthquakeDataLakeConverter converter;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public UsgsEarthquakeImportJob(MeterRegistry meterRegistry,
                                   UsgsEarthquakeClient client,
                                   DataLakeDao dataLakeDao,
                                   UsgsEarthquakeDataLakeConverter converter) {
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
                    String externalId = feature.get("id").asText();
                    JsonNode updatedNode = feature.path("properties").path("updated");
                    if (!updatedNode.isMissingNode() && StringUtils.isNotBlank(externalId)) {
                        OffsetDateTime updatedAt = DateTimeUtil.getDateTimeFromMilli(updatedNode.asLong());
                        enrichFeature(feature, externalId);
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

    private void enrichFeature(ObjectNode feature, String externalId) {
        try {
            JsonNode typesNode = feature.get("properties").get("types");
            String types = typesNode != null ? typesNode.asText() : null;
            boolean needShakemap = types != null && types.contains("shakemap");
            boolean needLoss = types != null && types.contains("losspager");
            if (!needShakemap && !needLoss) {
                return;
            }
            String detailJson = client.getDetail(externalId);
            if (StringUtils.isBlank(detailJson)) {
                if (needShakemap) {
                    feature.put("shakemap_cont_retrieval", false);
                    feature.put("shakemap_hishres_pga_retrieval", false);
                }
                if (needLoss) {
                    feature.put("loss_estimation_retrieval", false);
                }
                return;
            }
            JsonNode detail = JsonUtil.readTree(detailJson);
            if (needShakemap) {
                enrichWithShakemap(feature, detail, externalId);
            }
            if (needLoss) {
                enrichWithLossEstimation(feature, detail, externalId);
            }
        } catch (Exception e) {
            LOG.warn("Failed to enrich feature {}", externalId, e);
        }
    }

    private void enrichWithShakemap(ObjectNode feature, JsonNode detail, String externalId) {
        try {
            JsonNode shakemapArray = detail.at("/properties/products/shakemap");
            if (!shakemapArray.isArray() || shakemapArray.size() == 0) {
                feature.put("shakemap_cont_retrieval", false);
                feature.put("shakemap_hishres_pga_retrieval", false);
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
            JsonNode contents = first.get("contents");
            JsonNode contNode = contents != null ? contents.get("download/cont_mmi.json") : null;
            if (contNode != null) {
                String url = contNode.get("url").asText();
                feature.put("shm_url", url);
                String contPga = fetchUrl(url);
                if (contPga != null) {
                    result.set("download/cont_mmi.json", contNode);
                    try {
                        JsonNode contPgaNode = JsonUtil.readTree(contPga);
                        result.set("cont_mmi", contPgaNode);
                    } catch (Exception e) {
                        LOG.warn("Failed to parse cont_mmi.json for event {}", externalId, e);
                        feature.put("shakemap_cont_retrieval", false);
                        return;
                    }
                } else {
                    feature.put("shakemap_cont_retrieval", false);
                    return;
                }
            } else {
                feature.put("shakemap_cont_retrieval", false);
                return;
            }

            JsonNode contPgaHiNode = contents != null ? contents.get("download/cont_pga_highres.json") : null;
            if (contPgaHiNode != null) {
                String url = contPgaHiNode.get("url").asText();
                String contHiContent = fetchUrl(url);
                if (contHiContent != null) {
                    result.set("download/cont_pga_highres.json", contPgaHiNode);
                    try {
                        JsonNode contHi = JsonUtil.readTree(contHiContent);
                        result.set("cont_pga_highres", contHi);
                    } catch (Exception e) {
                        LOG.warn("Failed to parse cont_pga_highres.json for event {}", externalId, e);
                    }
                }
            }

            JsonNode hiResNode = contents != null ? contents.get("download/coverage_pga_high_res.covjson") : null;
            if (hiResNode != null) {
                String url = hiResNode.get("url").asText();
                String hiResContent = fetchUrl(url);
                if (hiResContent != null) {
                    result.set("download/coverage_pga_high_res.covjson", hiResNode);
                    try {
                        JsonNode hiRes = JsonUtil.readTree(hiResContent);
                        result.set("coverage_pga_high_res", hiRes);
                    } catch (Exception e) {
                        LOG.warn("Failed to parse coverage_pga_high_res.covjson for event {}", externalId, e);
                        feature.put("shakemap_hishres_pga_retrieval", false);
                    }
                } else {
                    feature.put("shakemap_hishres_pga_retrieval", false);
                }
            } else {
                feature.put("shakemap_hishres_pga_retrieval", false);
            }
            ArrayNode arr = feature.putArray("shakemap");
            arr.add(result);
        } catch (Exception e) {
            LOG.warn("Failed to enrich feature with shakemap", e);
            feature.put("shakemap_cont_retrieval", false);
            feature.put("shakemap_hishres_pga_retrieval", false);
        }
    }

    private void enrichWithLossEstimation(ObjectNode feature, JsonNode detail, String externalId) {
        try {
            JsonNode pagerArray = detail.at("/properties/products/losspager");
            if (!pagerArray.isArray() || pagerArray.size() == 0) {
                feature.put("loss_estimation_retrieval", false);
                return;
            }
            JsonNode first = pagerArray.get(0);
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
            JsonNode contents = first.get("contents");
            JsonNode lossNode = contents != null ? contents.get("json/losses.json") : null;
            if (lossNode != null) {
                String url = lossNode.get("url").asText();
                feature.put("loss_url", url);
                String body = fetchUrl(url);
                if (body != null) {
                    JsonNode loss = JsonUtil.readTree(body);
                    result.set("loss_estimations", loss);
                } else {
                    feature.put("loss_estimation_retrieval", false);
                    return;
                }
            } else {
                feature.put("loss_estimation_retrieval", false);
                return;
            }
            ArrayNode arr = feature.putArray("loss_estimation");
            arr.add(result);
        } catch (Exception e) {
            LOG.warn("Failed to enrich feature with loss estimation", e);
            feature.put("loss_estimation_retrieval", false);
        }
    }

    private void copyField(JsonNode from, ObjectNode to, String field) {
        JsonNode node = from.get(field);
        if (node != null) {
            to.set(field, node);
        }
    }

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
