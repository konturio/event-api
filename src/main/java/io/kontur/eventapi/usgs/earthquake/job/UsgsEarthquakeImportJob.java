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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class UsgsEarthquakeImportJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(UsgsEarthquakeImportJob.class);

    private final UsgsEarthquakeClient client;
    private final DataLakeDao dataLakeDao;
    private final UsgsEarthquakeDataLakeConverter converter;

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
}
