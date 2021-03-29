package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.tornado.client.NoaaTornadoClient;
import io.kontur.eventapi.tornado.service.NoaaTornadoImportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.kontur.eventapi.tornado.job.NoaaTornadoImportJob.TORNADO_NOAA_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;
import static org.junit.jupiter.api.Assertions.*;

class NoaaTornadoImportJobIT extends AbstractCleanableIntegrationTest {

    private static final String DATA_FILE = "io/kontur/eventapi/tornado/job/StormEvents_details.csv.gz";
    private static final OffsetDateTime UPDATED_AT = parseDateTimeFromString("1 Jan 2020 00:00:00 GMT");

    private final NoaaTornadoImportJob noaaTornadoImportJob;
    private final DataLakeDao dataLakeDao;

    @MockBean
    private NoaaTornadoClient noaaTornadoClient;
    @SpyBean
    private NoaaTornadoImportService noaaTornadoImportService;

    @Autowired
    public NoaaTornadoImportJobIT(JdbcTemplate jdbcTemplate, NoaaTornadoImportJob noaaTornadoImportJob,
                                     DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.noaaTornadoImportJob = noaaTornadoImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testNoaaTornadoImportJobRun() throws IOException {
        Mockito.when(noaaTornadoImportService.parseFilenamesAndUpdateDates()).thenReturn(Map.of(DATA_FILE, UPDATED_AT));
        byte[] gzip = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(DATA_FILE)).readAllBytes();
        Mockito.when(noaaTornadoClient.getGZIP(DATA_FILE)).thenReturn(gzip);
        noaaTornadoImportJob.run();

        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents();
        assertEquals(10, dataLakes.size());

        dataLakes.forEach(this::checkDataLake);
    }

    private void checkDataLake(DataLake dataLake) {
        assertNotNull(dataLake.getObservationId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getExternalId());
        assertNotNull(dataLake.getData());
        assertEquals(UPDATED_AT, dataLake.getUpdatedAt());
        assertEquals(TORNADO_NOAA_PROVIDER, dataLake.getProvider());
    }
}