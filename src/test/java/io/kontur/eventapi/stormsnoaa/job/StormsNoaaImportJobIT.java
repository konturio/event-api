package io.kontur.eventapi.stormsnoaa.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.stormsnoaa.client.StormsNoaaClient;
import io.kontur.eventapi.stormsnoaa.parser.FileInfo;
import io.kontur.eventapi.stormsnoaa.parser.StormsNoaaHTMLParser;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class StormsNoaaImportJobIT extends AbstractCleanableIntegrationTest {

    private final StormsNoaaImportJob stormsNoaaImportJob;
    private final DataLakeDao dataLakeDao;

    @MockBean
    private StormsNoaaClient stormsNoaaClient;

    @MockBean
    private StormsNoaaHTMLParser stormsNoaaHTMLParser;

    @Autowired
    public StormsNoaaImportJobIT(JdbcTemplate jdbcTemplate, StormsNoaaImportJob stormsNoaaImportJob,
                                 DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.stormsNoaaImportJob = stormsNoaaImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testNoaaTornadoImport() throws IOException {
        Mockito.when(stormsNoaaHTMLParser.parseFilesInfo())
                .thenReturn(List.of(new FileInfo("test-filename", OffsetDateTime.now())));
        Mockito.when(stormsNoaaClient.getGZIP("test-filename"))
                .thenReturn(getTestFile());
        stormsNoaaImportJob.run();
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents();
        assertEquals(2, dataLakes.size());
        checkDataLake(dataLakes.get(0), "10096222");
        checkDataLake(dataLakes.get(1), "10120412");
    }

    private byte[] getTestFile() throws IOException {
        String csv = "EPISODE_ID,EVENT_ID,STATE,CZ_NAME,BEGIN_DATE_TIME,END_DATE_TIME,DAMAGE_PROPERTY,TOR_F_SCALE,BEGIN_LAT,BEGIN_LON,END_LAT,END_LON,EPISODE_NARRATIVE,EVENT_NARRATIVE\n" +
                ",10096222,OKLAHOMA,WASHITA,28-APR-50 14:45:00,28-APR-50 14:45:00,250K,F3,35.12,-99.2,35.17,-99.2,,\n" +
                ",10120412,TEXAS,COMANCHE,29-APR-50 15:30:00,29-APR-50 15:30:00,25K,F1,31.9,-98.6,31.73,-98.6,,";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(csv.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        return out.toByteArray();
    }

    private void checkDataLake(DataLake dataLake, String externalId) {
        assertNotNull(dataLake.getObservationId());
        assertEquals(externalId, dataLake.getExternalId());
        assertNotNull(dataLake.getLoadedAt());
        assertNotNull(dataLake.getUpdatedAt());
        assertEquals("storms.noaa", dataLake.getProvider());
        assertNotNull(dataLake.getData());
    }
}