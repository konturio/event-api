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
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter.USGS_EARTHQUAKE_PROVIDER;

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
            FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            for (Feature feature : featureCollection.getFeatures()) {
                try {
                    String externalId = String.valueOf(feature.getId());
                    Object updatedObj = feature.getProperties().get("updated");
                    if (updatedObj != null && StringUtils.isNotBlank(externalId)) {
                        long updatedMilli = Long.parseLong(String.valueOf(updatedObj));
                        OffsetDateTime updatedAt = DateTimeUtil.getDateTimeFromMilli(updatedMilli);
                        if (Boolean.TRUE.equals(dataLakeDao.isNewEvent(externalId, USGS_EARTHQUAKE_PROVIDER,
                                updatedAt.format(DateTimeFormatter.ISO_INSTANT)))) {
                            dataLakes.add(converter.convert(externalId, updatedAt, JsonUtil.writeJson(feature)));
                        }
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
