package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.tornado.service.StaticTornadoImportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StaticTornadoImportJobIT extends AbstractCleanableIntegrationTest {

    private static final String PROVIDER = "test-provider";
    private static final String DATA_FILE = "io/kontur/eventapi/tornado/job/static_data.json";

    private final StaticTornadoImportJob staticTornadoImportJob;
    private final DataLakeDao dataLakeDao;

    @SpyBean
    private StaticTornadoImportService staticTornadoImportService;

    @Autowired
    public StaticTornadoImportJobIT(JdbcTemplate jdbcTemplate, StaticTornadoImportJob staticTornadoImportJob,
                                    DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.staticTornadoImportJob = staticTornadoImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testStaticTornadoImportJobRun() {
        Mockito.when(staticTornadoImportService.getProviderFilenames()).thenReturn(Map.of(PROVIDER, DATA_FILE));
        staticTornadoImportJob.run();
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents();
        assertEquals(3, dataLakes.size());
        dataLakes.forEach(this::checkDataLake);
    }

    private void checkDataLake(DataLake dataLake) {
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getExternalId());
        assertEquals(PROVIDER, dataLake.getProvider());
        assertNotNull(dataLake.getData());
    }
}