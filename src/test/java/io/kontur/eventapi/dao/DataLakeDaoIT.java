package io.kontur.eventapi.dao;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataLakeDaoIT extends AbstractCleanableIntegrationTest {

    private final DataLakeDao dataLakeDao;

    private final static String externalId = UUID.randomUUID().toString();

    private final static String featurePath1 = "DataLakeDaoIT.isNewExposure1.feature.json";
    private final static String geoHash1 = "1dmkgvnjvujx0phrejru77bpbpbpbpbpbpbp";
    private final static OffsetDateTime loadedAt1 = OffsetDateTime.parse("2021-05-10T00:00:00Z");

    private final static String featurePath2 = "DataLakeDaoIT.isNewExposure2.feature.json";
    private final static String geoHash2 = "46w9vyfy3kvgc67z43n0v6pbpbpbpbpbpbpb";
    private final static OffsetDateTime loadedAt2 = OffsetDateTime.parse("2021-05-20T00:00:00Z");

    private final static String featurePath3 = "DataLakeDaoIT.isNewExposure3.feature.json";
    private final static String geoHash3 = "u0r1qzvzvq02dg9r0fqsm9bzzzzzzzzzzzzz";
    private final static OffsetDateTime loadedAt3 = OffsetDateTime.parse("2021-05-25T00:00:00Z");

    @Autowired
    public DataLakeDaoIT(JdbcTemplate jdbcTemplate, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testIsNewExposure() throws IOException {
        String feature1 = readFile(featurePath1);

        DataLake dataLake1 = generateDataLake(feature1, loadedAt1);
        dataLakeDao.storeEventData(dataLake1);

        assertFalse(dataLakeDao.isNewPdcExposure(externalId, geoHash1));
        assertTrue(dataLakeDao.isNewPdcExposure(externalId, geoHash2));
    }

    @Test
    public void testIsNewExposureWhenNoRecordsInDB() {
        assertTrue(dataLakeDao.isNewPdcExposure(externalId, geoHash1));
    }

    @Test
    public void testIsNewExposureWhenExposureWithSameGeometryIsNotLastUpdated() throws IOException {
        DataLake dataLake1 = generateDataLake(readFile(featurePath1), loadedAt1);
        DataLake dataLake2 = generateDataLake(readFile(featurePath2), loadedAt2);
        DataLake dataLake3 = generateDataLake(readFile(featurePath3), loadedAt3);

        dataLakeDao.storeDataLakes(List.of(dataLake1, dataLake2, dataLake3));

        assertFalse(dataLakeDao.isNewPdcExposure(externalId, geoHash1));
        assertFalse(dataLakeDao.isNewPdcExposure(externalId, geoHash2));
        assertFalse(dataLakeDao.isNewPdcExposure(externalId, geoHash3));
    }

    public DataLake generateDataLake(String data, OffsetDateTime loadedAt) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, loadedAt, loadedAt);
        dataLake.setProvider("pdcMapSrv");
        dataLake.setData(data);
        return dataLake;
    }

    private String readFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}