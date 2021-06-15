package io.kontur.eventapi.gdacs.job;

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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

class GdacsSearchJobIT extends AbstractCleanableIntegrationTest {
    private final DataLakeDao dataLakeDao;

    private final GdacsClient gdacsClient = mock(GdacsClient.class);
    private final GdacsSearchJob gdacsSearchJob;

    @Autowired
    public GdacsSearchJobIT(JdbcTemplate jdbcTemplate, DataLakeDao dataLakeDao) {
        super(jdbcTemplate);
        GdacsService gdacsService = new GdacsService(dataLakeDao, new GdacsDataLakeConverter(), gdacsClient);
        this.gdacsSearchJob = new GdacsSearchJob(gdacsService, new GdacsAlertXmlParser(), new SimpleMeterRegistry());
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testJob() throws IOException {
        String alert = readMessageFromFile("alert01.xml");
        String geometry = readMessageFromFile("geometry01.json");

        Mockito.when(gdacsClient.getXml()).thenReturn(readMessageFromFile("cap1.xml"));
        Mockito.when(gdacsClient.getAlertByLink(anyString())).thenReturn(alert);
        Mockito.when(gdacsClient.getGeometryByLink("EQ", "1243255", "1342589")).thenReturn(geometry);

        gdacsSearchJob.run();

        checkDataLakes(dataLakeDao.getDenormalizedEvents(), alert, geometry);
    }

    public void testJobWhenNoGeometryForAlert() throws IOException {
        String alert1 = readMessageFromFile("alert01.xml");
        String alert2 = readMessageFromFile("alert02.xml");
        String geometry = readMessageFromFile("geometry01.json");

        Mockito.when(gdacsClient.getXml()).thenReturn(readMessageFromFile("cap2.xml"));
        Mockito.when(gdacsClient.getAlertByLink("https://www.gdacs.org/contentdata/resources/EQ/1243255/cap_1243255.xml")).thenReturn(alert1);
        Mockito.when(gdacsClient.getAlertByLink("https://www.gdacs.org/contentdata/resources/TC/1000742/cap_1000742.xml")).thenReturn(alert2);
        Mockito.when(gdacsClient.getGeometryByLink("EQ", "1243255", "1342589")).thenReturn(geometry);

        gdacsSearchJob.run();

        checkDataLakes(dataLakeDao.getDenormalizedEvents(), alert1, geometry);
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

    private void checkDataLakes(List<DataLake> dataLakes, String alert, String geometry) {
        assertEquals(2, dataLakes.size());

        DataLake alertDataLake = dataLakes.get(0);
        DataLake geometryDataLake = dataLakes.get(1);

        assertEquals("GDACS_EQ_1243255_1342589", alertDataLake.getExternalId());
        assertEquals(OffsetDateTime.parse("Tue, 10 Nov 2020 06:07:49 GMT", DateTimeFormatter.RFC_1123_DATE_TIME), alertDataLake.getUpdatedAt());
        assertEquals(GDACS_ALERT_PROVIDER, alertDataLake.getProvider());
        assertEquals(alert, alertDataLake.getData());

        assertEquals("GDACS_EQ_1243255_1342589", geometryDataLake.getExternalId());
        assertEquals(OffsetDateTime.parse("Tue, 10 Nov 2020 06:07:49 GMT", DateTimeFormatter.RFC_1123_DATE_TIME), geometryDataLake.getUpdatedAt());
        assertEquals(GDACS_ALERT_GEOMETRY_PROVIDER, geometryDataLake.getProvider());
        assertEquals(geometry, geometryDataLake.getData());
    }

}