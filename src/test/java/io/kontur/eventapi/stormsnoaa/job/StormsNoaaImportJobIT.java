package io.kontur.eventapi.stormsnoaa.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.stormsnoaa.service.StormsNoaaImportService;
import io.kontur.eventapi.stormsnoaa.parser.FileInfo;
import io.kontur.eventapi.stormsnoaa.parser.StormsNoaaHTMLParser;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob.STORMS_NOAA_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;

class StormsNoaaImportJobIT extends AbstractCleanableIntegrationTest {

    private final StormsNoaaImportJob stormsNoaaImportJob;
    private final DataLakeDao dataLakeDao;

    private final String testFilename = "test-file.csv.gz";

    @Value("${stormsNoaa.host}")
    private String URL;

    @MockBean
    private StormsNoaaImportService stormsNoaaImportService;

    @MockBean
    private StormsNoaaHTMLParser stormsNoaaHTMLParser;

    @Autowired
    public StormsNoaaImportJobIT(JdbcTemplate jdbcTemplate, StormsNoaaImportJob stormsNoaaImportJob,
                                 DataLakeDao dataLakeDao, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.stormsNoaaImportJob = stormsNoaaImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testNoaaTornadoImport() throws Exception {
        String testFilePath = createTestFile();
        Mockito.when(stormsNoaaHTMLParser.parseFilesInfo())
                .thenReturn(List.of(new FileInfo(testFilename, OffsetDateTime.now())));
        Mockito.doNothing().when(stormsNoaaImportService).downloadFile(isA(String.class), isA(String.class));
        Mockito.when(stormsNoaaImportService.getFilePath(isA(String.class))).thenReturn(testFilePath);
        stormsNoaaImportJob.run();
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(List.of(STORMS_NOAA_PROVIDER));
        assertEquals(2, dataLakes.size());
        checkDataLake(dataLakes.get(0), "10096222");
        checkDataLake(dataLakes.get(1), "10120412");
    }

    private String createTestFile() throws IOException {
        String csv = "EPISODE_ID,EVENT_ID,STATE,CZ_NAME,BEGIN_DATE_TIME,END_DATE_TIME,DAMAGE_PROPERTY,TOR_F_SCALE,BEGIN_LAT,BEGIN_LON,END_LAT,END_LON,EPISODE_NARRATIVE,EVENT_NARRATIVE\n" +
                ",10096222,OKLAHOMA,WASHITA,28-APR-50 14:45:00,28-APR-50 14:45:00,250K,F3,35.12,-99.2,35.17,-99.2,,\n" +
                ",10120412,TEXAS,COMANCHE,29-APR-50 15:30:00,29-APR-50 15:30:00,25K,F1,31.9,-98.6,31.73,-98.6,,";
        String testFilePath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + testFilename;
        try (FileOutputStream fileOutputStream = new FileOutputStream(testFilePath);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream)) {
            outputStreamWriter.write(csv);
        }
        return testFilePath;
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