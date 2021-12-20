package io.kontur.eventapi.firms.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.firms.jobs.FirmsImportModisJob;
import io.kontur.eventapi.firms.jobs.FirmsImportNoaaJob;
import io.kontur.eventapi.firms.jobs.FirmsImportSuomiJob;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.firms.client.FirmsClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static io.kontur.eventapi.TestUtil.readFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FirmsImportJobIT extends AbstractCleanableIntegrationTest {
    private final FirmsImportModisJob firmsImportModisJob;
    private final FirmsImportNoaaJob firmsImportNoaaJob;
    private final FirmsImportSuomiJob firmsImportSuomiJob;
    private final DataLakeDao dataLakeDao;

    @MockBean
    private FirmsClient firmsClient;

    @Autowired
    public FirmsImportJobIT(FirmsImportModisJob firmsImportModisJob, FirmsImportNoaaJob firmsImportNoaaJob,
                            FirmsImportSuomiJob firmsImportSuomiJob, DataLakeDao dataLakeDao, JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.firmsImportModisJob = firmsImportModisJob;
        this.firmsImportNoaaJob = firmsImportNoaaJob;
        this.firmsImportSuomiJob = firmsImportSuomiJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testNormalImport() throws IOException {
        //given
        Mockito.when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6.csv"));
        Mockito.when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        Mockito.when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));

        //when
        firmsImportModisJob.run();
        firmsImportNoaaJob.run();
        firmsImportSuomiJob.run();

        //then
        //check modis-c6
        List<DataLake> modisDataLakes = dataLakeDao.getDataLakesByExternalId("e78338918b55f933f6bc3f5d3f235a73");
        assertEquals(1, modisDataLakes.size());
        DataLake dataLake = modisDataLakes.get(0);
        assertEquals("e78338918b55f933f6bc3f5d3f235a73", dataLake.getExternalId());
        assertEquals("firms.modis-c6", dataLake.getProvider());
        assertEquals(OffsetDateTime.parse("2020-11-02T12:50Z"), dataLake.getUpdatedAt());
        assertNotNull(dataLake.getLoadedAt());
        assertEquals("latitude,longitude,brightness,scan,track,acq_date,acq_time,satellite,confidence,version,bright_t31,frp,daynight\n" +
                "-14.674,142.397,306.7,1.5,1.2,2020-11-02,1250,T,41,6.0NRT,296.3,7.1,N", dataLake.getData());

        //check noaa-20-viirs-c2
        List<DataLake> suomiDataLakes = dataLakeDao.getDataLakesByExternalId("7a5b1d79458295d2659d91eb16749243");
        assertEquals(1, suomiDataLakes.size());
        DataLake suomiDataLake = suomiDataLakes.get(0);
        assertEquals("7a5b1d79458295d2659d91eb16749243", suomiDataLake.getExternalId());
        assertEquals("firms.noaa-20-viirs-c2", suomiDataLake.getProvider());
        assertNotNull(suomiDataLake.getLoadedAt());
        assertEquals(OffsetDateTime.parse("2020-11-02T11:00Z"), suomiDataLake.getUpdatedAt());
        assertEquals("latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,confidence,version,bright_ti5,frp,daynight\n" +
                "44.48262,-119.91124,297.8,0.51,0.66,2020-11-02,1100,1,nominal,2.0NRT,277.5,0.9,N", suomiDataLake.getData());

        //check suomi-npp-viirs-c2
        List<DataLake> noaaDataLakes = dataLakeDao.getDataLakesByExternalId("2468a53b16dcb4c4ca3b6043fac822cb");
        assertEquals(1, noaaDataLakes.size());
        DataLake noaaDataLake = noaaDataLakes.get(0);
        assertEquals("2468a53b16dcb4c4ca3b6043fac822cb", noaaDataLake.getExternalId());
        assertEquals("firms.suomi-npp-viirs-c2", noaaDataLake.getProvider());
        assertNotNull(noaaDataLake.getLoadedAt());
        assertEquals(OffsetDateTime.parse("2020-11-02T11:06Z"), noaaDataLake.getUpdatedAt());
        assertEquals("latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,confidence,version,bright_ti5,frp,daynight\n" +
                "-15.35001,40.51532,346.9,0.56,0.43,2020-11-02,1106,N,nominal,2.0NRT,302.7,9.2,D", noaaDataLake.getData());
    }


    @Test
    public void testImportUpdates() throws IOException {
        //given
        // import first time
        Mockito.when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6.csv"));
        Mockito.when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        Mockito.when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));

        firmsImportModisJob.run();
        firmsImportNoaaJob.run();
        firmsImportSuomiJob.run();

        Mockito.reset(firmsClient);

        //new data available for modis
        Mockito.when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6.update.csv"));
        Mockito.when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        Mockito.when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));

        //when import second time
        firmsImportModisJob.run();
        firmsImportNoaaJob.run();
        firmsImportSuomiJob.run();

        //then
        //old data still here without duplicates
        List<DataLake> dataLakes = dataLakeDao.getDataLakesByExternalId("e78338918b55f933f6bc3f5d3f235a73");
        assertEquals(1, dataLakes.size());

        //new data is imported
        List<DataLake> newDataLakes = dataLakeDao.getDataLakesByExternalId("78d8da2dad1cbfa2882b9d8cb628c939");
        assertEquals(1, newDataLakes.size());
        DataLake newdataLake = newDataLakes.get(0);
        assertEquals("78d8da2dad1cbfa2882b9d8cb628c939", newdataLake.getExternalId());
        assertEquals("firms.modis-c6", newdataLake.getProvider());
        assertEquals(OffsetDateTime.parse("2020-11-02T12:59Z"), newdataLake.getUpdatedAt());
        assertNotNull(newdataLake.getLoadedAt());
        assertEquals("latitude,longitude,brightness,scan,track,acq_date,acq_time,satellite,confidence,version,bright_t31,frp,daynight\n" +
                "-12.341,142.889,306.8,1.3,1.1,2020-11-02,1259,T,54,6.0NRT,291.9,7.5,N", newdataLake.getData());
    }


    private String readCsv(String fileName) throws IOException {
        return readFile(this,fileName);
    }

}