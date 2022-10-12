package io.kontur.eventapi.emdat.jobs;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.emdat.dto.EmDatPublicFile;
import io.kontur.eventapi.emdat.service.EmDatImportService;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static io.kontur.eventapi.emdat.jobs.EmDatImportJob.EM_DAT_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmDatImportJobIT extends AbstractCleanableIntegrationTest {

    private final EmDatImportJob emDatImportJob;
    private final DataLakeDao dataLakeDao;

    @MockBean
    private EmDatImportService importService;

    @Autowired
    public EmDatImportJobIT(JdbcTemplate jdbcTemplate,
                            EmDatImportJob emDatImportJob, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        this.emDatImportJob = emDatImportJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testFileImport() {
        //given
        prepareService("em-dat.test1.xlsx");

        //when
        emDatImportJob.run();

        //then
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(List.of(EM_DAT_PROVIDER));
        assertEquals(3, dataLakes.size());

        DataLake emdat1 = dataLakes.get(0);
        checkDataLakeEntity(emdat1, "2020-0020-FRA",
                "2020-0020-FRA,2020,0020,Natural,Meteorological,Storm,Extra-tropical storm,,'Gloria',Affected,France,FRA,Western Europe,Europe,\"Pyrénées-Orientales, Aude, Ariège, and Haute-Garonne departments\",,,,,,,,,Kph,,,,,2020,1,21,2020,1,22");

        DataLake emdat2 = dataLakes.get(1);
        checkDataLakeEntity(emdat2, "2020-0207-CUB",
                "2020-0207-CUB,2020,0207,Natural,Meteorological,Storm,Convective storm,,,Affected,Cuba,CUB,Caribbean,Americas,\"Sancti Spiritus, Villa Clara, Cienfuegos, Camaguey, Ciego de Avila provinces (central Cuba)\",,Flood,,,,,,120,Kph,,,,,2020,5,20,2020,5,25,,3,3830,,3833");

        DataLake emdat3 = dataLakes.get(2);
        checkDataLakeEntity(emdat3, "1900-9002-CPV",
                "1900-9002-CPV,1900,9002,Natural,Climatological,Drought,Drought,,,,Cabo Verde,CPV,Western Africa,Africa,Countrywide,,Famine,,,No,No,,,Km2,,,,,1900,,,1900,,,11000,,,,,,,,3.2613889831938998");
    }

    @Test
    public void testUpdatedFileImport() {
        //given
        testFileImport(); //import file for the first time
        prepareService("em-dat.test2.xlsx");

        //when
        emDatImportJob.run();

        //then
        List<DataLake> dataLakes = dataLakeDao.getDenormalizedEvents(List.of(EM_DAT_PROVIDER));
        assertEquals(4, dataLakes.size());

        DataLake emdat1 = dataLakes.get(0);
        checkDataLakeEntity(emdat1, "2020-0020-FRA",
                "2020-0020-FRA,2020,0020,Natural,Meteorological,Storm,Extra-tropical storm,,'Gloria',Affected,France,FRA,Western Europe,Europe,\"Pyrénées-Orientales, Aude, Ariège, and Haute-Garonne departments\",,,,,,,,,Kph,,,,,2020,1,21,2020,1,22");

        DataLake emdat2 = dataLakes.get(1);
        checkDataLakeEntity(emdat2, "2020-0207-CUB",
                "2020-0207-CUB,2020,0207,Natural,Meteorological,Storm,Convective storm,,,Affected,Cuba,CUB,Caribbean,Americas,\"Sancti Spiritus, Villa Clara, Cienfuegos, Camaguey, Ciego de Avila provinces (central Cuba)\",,Flood,,,,,,120,Kph,,,,,2020,5,20,2020,5,25,,3,3830,,3833");

        DataLake emdat3 = dataLakes.get(2);
        checkDataLakeEntity(emdat3, "1900-9002-CPV",
                "1900-9002-CPV,1900,9002,Natural,Climatological,Drought,Drought,,,,Cabo Verde,CPV,Western Africa,Africa,Countrywide,,Famine,,,No,No,,,Km2,,,,,1900,,,1900,,,11000,,,,,,,,3.2613889831938998");

        DataLake emdat4 = dataLakes.get(3);
        checkDataLakeEntity(emdat4, "2020-0131-TLS",
                "2020-0131-TLS,2020,0131,Natural,Hydrological,Flood,Riverine flood,,,Affected,Timor-Leste,TLS,South-Eastern Asia,Asia,\"Cristo Rei, Nain Feto, Dom Aleixo, and Vera Cruz (Dili municipality)\",Heavy rains,,,,,,,,Km2,,,,,2020,3,13,2020,3,13,,7,9124,,9131,,,20000");
    }

    private void checkDataLakeEntity(DataLake dataLake, String externalId, String dataRow) {
        assertEquals(externalId, dataLake.getExternalId());
        assertEquals(EM_DAT_PROVIDER, dataLake.getProvider());
        assertEquals(
                "Dis No,Year,Seq,Disaster Group,Disaster Subgroup,Disaster Type,Disaster Subtype,Disaster Subsubtype,Event Name,Entry Criteria,Country,ISO,Region,Continent,Location,Origin,Associated Dis,Associated Dis2,OFDA Response,Appeal,Declaration,Aid Contribution,Dis Mag Value,Dis Mag Scale,Latitude,Longitude,Local Time,River Basin,Start Year,Start Month,Start Day,End Year,End Month,End Day,Total Deaths,No Injured,No Affected,No Homeless,Total Affected,Reconstruction Costs ('000 US$),Insured Damages ('000 US$),Total Damages ('000 US$),CPI\n" +
                        dataRow,
                dataLake.getData());
        assertNotNull(dataLake.getUpdatedAt());
        assertNotNull(dataLake.getLoadedAt());
    }

    private void prepareService(String fileName) {
        Mockito.when(importService.obtainAuthToken()).thenReturn("token");
        EmDatPublicFile emDatPublicFile = new EmDatPublicFile();
        emDatPublicFile.setName(fileName);
        Mockito.when(importService.obtainFileStatistic("token")).thenReturn(emDatPublicFile);
        Mockito.when(importService.obtainFile(fileName, "token")).thenReturn(getClass().getResourceAsStream(fileName));
    }
}