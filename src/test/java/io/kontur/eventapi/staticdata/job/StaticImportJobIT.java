package io.kontur.eventapi.staticdata.job;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.staticdata.service.AwsS3Service;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;

class StaticImportJobIT extends AbstractCleanableIntegrationTest {

    private final StaticImportJob staticImportJob;
    private final DataLakeDao dataLakeDao;

    private final static String testFile = "test.geojson";
    private final static String provider = "test-provider";
    private final static ObjectMetadata metadata = new ObjectMetadata();
    static {
        metadata.setLastModified(new Date());
        metadata.setUserMetadata(Map.of("provider", provider));
    }

    @MockBean
    private S3Object s3Object;

    @MockBean
    private AwsS3Service awsS3Service;

    @Autowired
    public StaticImportJobIT(JdbcTemplate jdbcTemplate, StaticImportJob staticImportJob, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.staticImportJob = staticImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testStaticDataImport() {
        Mockito.when(awsS3Service.listS3ObjectKeys()).thenReturn(List.of(testFile));
        Mockito.when(awsS3Service.getS3Object(isA(String.class))).thenReturn(s3Object);
        Mockito.when(s3Object.getObjectMetadata()).thenReturn(metadata);
        Mockito.when(s3Object.getObjectContent())
                .thenReturn(new S3ObjectInputStream(getClass().getResourceAsStream(testFile), new HttpGet()));
        staticImportJob.run();
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(List.of(provider));
        assertEquals(3, dataLakes.size());
        checkDataLakeFields(dataLakes.get(0));
        checkDataLakeFields(dataLakes.get(1));
        checkDataLakeFields(dataLakes.get(2));
    }

    private void checkDataLakeFields(DataLake dataLake) {
        assertEquals(provider, dataLake.getProvider());
        assertNotNull(dataLake.getUpdatedAt());
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getExternalId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getData());
    }
}