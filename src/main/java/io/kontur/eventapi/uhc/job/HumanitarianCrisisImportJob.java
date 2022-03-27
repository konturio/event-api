package io.kontur.eventapi.uhc.job;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.model.S3Object;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.uhc.converter.UHCDataLakeConverter;
import io.kontur.eventapi.uhc.service.S3Service;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

@Component
public class HumanitarianCrisisImportJob extends AbstractJob {
    private static final Logger LOG = LoggerFactory.getLogger(HumanitarianCrisisImportJob.class);

    private static final int MAX_DATA_LAKES_COUNT = 1000;

    private final S3Service s3Service;

    private final DataLakeDao dataLakeDao;

    private final UHCDataLakeConverter converter;

    public HumanitarianCrisisImportJob(MeterRegistry meterRegistry, S3Service s3Service, DataLakeDao dataLakeDao,
                                       UHCDataLakeConverter converter) {
        super(meterRegistry);
        this.s3Service = s3Service;
        this.dataLakeDao = dataLakeDao;
        this.converter = converter;
    }

    @Override
    public String getName() {
        return "humanitarianCrisisImport";
    }

    @Override
    public void execute() throws Exception {
        List<String> keys = s3Service.listS3ObjectKeys();
        for (String key : keys) {
            try (S3Object s3Object = s3Service.getS3Object(key)) {
                processFile(s3Object.getObjectContent());
            }
        }
    }

    void processFile(InputStream content) throws IOException {
        FeatureCollection featureCollection =
                (FeatureCollection) GeoJSONFactory.create(IOUtils.toString(content, StandardCharsets.UTF_8.name()));
        List<DataLake> dataLakes = new ArrayList<>();
        for (Feature feature : featureCollection.getFeatures()) {
            try {
                DataLake dataLake = converter.convertEvent(feature, dataLakeDao);
                if (dataLake != null) {
                    dataLakes.add(dataLake);
                    if (dataLakes.size() > MAX_DATA_LAKES_COUNT) {
                        dataLakeDao.storeDataLakes(dataLakes);
                        dataLakes.clear();
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error while processing Humanitarian Crisis feature. {}", e.getMessage());
            }
        }
        if (!CollectionUtils.isEmpty(dataLakes)) {
            dataLakeDao.storeDataLakes(dataLakes);
        }
    }

}
