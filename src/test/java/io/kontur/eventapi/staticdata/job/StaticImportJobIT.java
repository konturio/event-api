package io.kontur.eventapi.staticdata.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.staticdata.service.AwsS3Service;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StaticImportJobIT extends AbstractCleanableIntegrationTest {

    private final StaticImportJob staticImportJob;
    private final DataLakeDao dataLakeDao;

    private final static String testFile = "test.geojson";
    private final static String provider = "test-provider";
    private final static String updatedAt = "2020-07-12T00:00:00Z";

    @MockBean
    private AwsS3Service awsS3Service;

    @Autowired
    public StaticImportJobIT(JdbcTemplate jdbcTemplate, StaticImportJob staticImportJob, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.staticImportJob = staticImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testStaticDataImport() throws IOException {
        Mockito.when(awsS3Service.listS3ObjectKeys()).thenReturn(List.of(testFile));
        Mockito.when(awsS3Service.getS3ObjectContent(testFile)).thenReturn(getTestFileContent());
        Mockito.when(awsS3Service.getS3ObjectMetadata(testFile))
                .thenReturn(Map.of("updated-at", updatedAt, "provider", provider));

        staticImportJob.run();
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents();

        assertEquals(3, dataLakes.size());
        checkDataLakeFields(dataLakes.get(0));
        checkDataLakeFields(dataLakes.get(1));
        checkDataLakeFields(dataLakes.get(2));
    }

    private void checkDataLakeFields(DataLake dataLake) {
        assertEquals(provider, dataLake.getProvider());
        assertEquals(OffsetDateTime.parse(updatedAt), dataLake.getUpdatedAt());
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getExternalId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getData());
    }

    private String getTestFileContent() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(testFile);
        if (inputStream != null) {
            return new String(inputStream.readAllBytes());
        }
        throw new IOException("Test file not found: " + testFile);
    }
}