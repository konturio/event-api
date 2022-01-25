package io.kontur.eventapi.gdacs.job;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class GdacsSearchJobIT extends AbstractCleanableIntegrationTest {
    private final DataLakeDao dataLakeDao;

    private final GdacsClient gdacsClient = mock(GdacsClient.class);
    private final GdacsSearchJob gdacsSearchJob;

    @Autowired
    public GdacsSearchJobIT(JdbcTemplate jdbcTemplate, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        GdacsService gdacsService = new GdacsService(dataLakeDao, new GdacsDataLakeConverter(), gdacsClient);
        this.gdacsSearchJob = new GdacsSearchJob(gdacsService, new GdacsAlertXmlParser(), dataLakeDao, new SimpleMeterRegistry());
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testJob() throws IOException {
        String geometry = readMessageFromFile("geometry01.json");

        Mockito.when(gdacsClient.getXml()).thenReturn(readMessageFromFile("cap2.xml"));
        Mockito.when(gdacsClient.getGeometryByLink("EQ", "1279052", "1387993")).thenReturn(geometry);

        gdacsSearchJob.run();

        checkDataLakes(dataLakeDao.getDenormalizedEvents());
    }

    @Test
    public void testJobWhenNoGeometryForAlert() throws IOException {
        Mockito.when(gdacsClient.getXml()).thenReturn(readMessageFromFile("cap2.xml"));
        Mockito.when(gdacsClient.getGeometryByLink(any(), any(), any())).thenThrow(FeignException.class);

        gdacsSearchJob.run();

        assertEquals(0, dataLakeDao.getDenormalizedEvents().size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

    private void checkDataLakes(List<DataLake> dataLakes) {
        assertEquals(2, dataLakes.size());

        DataLake alertDataLake = dataLakes.get(0);
        DataLake geometryDataLake = dataLakes.get(1);

        assertEquals("GDACS_EQ_1279052_1387993", alertDataLake.getExternalId());
        assertEquals(OffsetDateTime.parse("Thu, 29 Jul 2021 07:16 GMT", DateTimeFormatter.RFC_1123_DATE_TIME), alertDataLake.getUpdatedAt());
        assertEquals(GDACS_ALERT_PROVIDER, alertDataLake.getProvider());
        assertNotNull(alertDataLake.getData());

        assertEquals("GDACS_EQ_1279052_1387993", geometryDataLake.getExternalId());
        assertEquals(OffsetDateTime.parse("Thu, 29 Jul 2021 07:16 GMT", DateTimeFormatter.RFC_1123_DATE_TIME), geometryDataLake.getUpdatedAt());
        assertEquals(GDACS_ALERT_GEOMETRY_PROVIDER, geometryDataLake.getProvider());
        assertNotNull(geometryDataLake.getData());
    }

}