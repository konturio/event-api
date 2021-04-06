package io.kontur.eventapi.staticdata.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.staticdata.config.StaticFileData;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticImportJobIT extends AbstractCleanableIntegrationTest {

    private final StaticImportJob staticImportJob;
    private final DataLakeDao dataLakeDao;

    private final static String provider = "test-provider";
    private final static OffsetDateTime updatedAt = OffsetDateTime.parse("2020-07-12T00:00:00Z");

    @Autowired
    public StaticImportJobIT(JdbcTemplate jdbcTemplate, StaticImportJob staticImportJob, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.staticImportJob = staticImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testStaticDataImport() throws IOException {
        ReflectionTestUtils.setField(staticImportJob, "files", List.of(
                new StaticFileData("io/kontur/eventapi/staticdata/static/test.geojson", provider,
                        updatedAt, "geojson")));

        staticImportJob.run();
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents();

        assertEquals(3, dataLakes.size());
        checkDataLakeFields(dataLakes.get(0));
        checkDataLakeFields(dataLakes.get(1));
        checkDataLakeFields(dataLakes.get(2));
    }

    private void checkDataLakeFields(DataLake dataLake) {
        assertEquals(provider, dataLake.getProvider());
        assertEquals(updatedAt, dataLake.getUpdatedAt());
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getExternalId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getData());
    }
}