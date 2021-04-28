package io.kontur.eventapi.staticdata.job;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.staticdata.service.AwsS3Service;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Component;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class StaticImportJob extends AbstractJob {
    private final AwsS3Service awsS3Service;
    private final DataLakeDao dataLakeDao;

    private static final int maxDataLakesCount = 1000;
    private static final FeatureJSON featureJson = new FeatureJSON();
    static {
        featureJson.setEncodeNullValues(true);
    }

    public StaticImportJob(MeterRegistry meterRegistry, AwsS3Service awsS3Service, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.awsS3Service = awsS3Service;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public String getName() {
        return "staticDataImport";
    }

    @Override
    public void execute() throws Exception {
        List<String> keys = awsS3Service.listS3ObjectKeys();
        for (String key : keys) {
            try (S3Object s3Object = awsS3Service.getS3Object(key)) {
                ObjectMetadata metadata = s3Object.getObjectMetadata();
                OffsetDateTime updatedAt = convertDate(metadata.getLastModified());
                String provider = metadata.getUserMetaDataOf("provider");
                processFile(provider, updatedAt, s3Object.getObjectContent());
            }
        }
    }

    private void processFile(String provider, OffsetDateTime updatedAt, InputStream content) throws IOException {
        FeatureIterator<SimpleFeature> features = featureJson.streamFeatureCollection(content);
        List<DataLake> dataLakes = new ArrayList<>();
        while (features.hasNext()) {
            try (OutputStream outputStream = new ByteArrayOutputStream()) {
                SimpleFeature feature = features.next();
                featureJson.writeFeature(feature, outputStream);
                String data = outputStream.toString();
                String externalId = DigestUtils.md5Hex(data);
                if (dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, provider).isEmpty()) {
                    dataLakes.add(createDataLake(externalId, updatedAt, provider, data));
                }
            }
            if (dataLakes.size() > maxDataLakesCount) {
                dataLakeDao.storeDataLakes(dataLakes);
                dataLakes.clear();
            }
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }

    private DataLake createDataLake(String externalId, OffsetDateTime updatedAt, String provider, String data) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(provider);
        dataLake.setData(data);
        return dataLake;
    }

    private OffsetDateTime convertDate(Date date) {
        return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }
}
