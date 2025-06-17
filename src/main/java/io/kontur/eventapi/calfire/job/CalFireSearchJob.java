package io.kontur.eventapi.calfire.job;

import io.kontur.eventapi.calfire.client.CalFireClient;
import io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;

@Component
public class CalFireSearchJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(CalFireSearchJob.class);
    private final CalFireClient calFireClient;
    private final CalFireDataLakeConverter calFireDataLakeConverter;
    private final DataLakeDao dataLakeDao;

    protected CalFireSearchJob(MeterRegistry meterRegistry, CalFireClient calFireClient,
                               CalFireDataLakeConverter calFireDataLakeConverter, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.calFireClient = calFireClient;
        this.calFireDataLakeConverter = calFireDataLakeConverter;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public String getName() {
        return "calfireSearch";
    }

    @Override
    public void execute() throws Exception {
        try {
            String geoJson = calFireClient.getEvents();
            FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJson);
            List<DataLake> dataLakes = new ArrayList<>();
            for (Feature feature : featureCollection.getFeatures()) {
                try {
                    String externalId = String.valueOf(feature.getProperties().get("UniqueId"));
                    String updatedAtValue = (String) feature.getProperties().get("Updated");
                    if (StringUtils.isNotBlank(updatedAtValue)) {
                        OffsetDateTime updatedAt = DateTimeUtil.parseDateTimeByPattern(updatedAtValue, null);
                        if (updatedAt != null && dataLakeDao.isNewEvent(externalId, CALFIRE_PROVIDER,
                                updatedAt.format(DateTimeFormatter.ISO_INSTANT))) {
                            dataLakes.add(
                                    calFireDataLakeConverter.convertEvent(feature.toString(), externalId, updatedAt));
                        }
                    }
                } catch (Exception e1) {
                    LOG.warn("Error while processing calfire feature. {}", e1.getMessage());
                }
            }
            if (!CollectionUtils.isEmpty(dataLakes)) {
                dataLakeDao.storeDataLakes(dataLakes);
            }
        } catch (Exception e) {
            LOG.warn("Error while obtaining and processing calfire features. {}", e.getMessage());
        }

    }

}
