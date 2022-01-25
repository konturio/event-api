package io.kontur.eventapi.gdacs.service;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.util.DateTimeUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GdacsServiceTest {

    private final DataLakeDao dataLakeDao = mock(DataLakeDao.class);
    private final GdacsClient gdacsClient = mock(GdacsClient.class);
    private final GdacsDataLakeConverter gdacsDataLakeConverter = mock(GdacsDataLakeConverter.class);

    private final GdacsService gdacsService;

    GdacsServiceTest() {
        this.gdacsService = new GdacsService(dataLakeDao, gdacsDataLakeConverter, gdacsClient);
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(dataLakeDao);
        Mockito.reset(gdacsClient);
        Mockito.reset(gdacsDataLakeConverter);
    }

    @Test
    public void testFetchGdacsXml() {
        when(gdacsClient.getXml()).thenReturn("test-string");
        Optional<String> resultXml = gdacsService.fetchGdacsXml();
        assertTrue(resultXml.isPresent());
    }

    @Test
    public void testFetchGdacsXmlWhenThrows() {
        when(gdacsClient.getXml()).thenThrow(FeignException.class);
        Optional<String> resultXml = gdacsService.fetchGdacsXml();
        assertTrue(resultXml.isEmpty());
    }

    @Test
    public void testCreateDataLakeListWithAlertsAndGeometry() throws IOException {
        ParsedAlert alert = getParsedAlert();
        String geometry = readFile("geometry.json");

        when(dataLakeDao.getDataLakesByExternalId(alert.getIdentifier())).thenReturn(Collections.emptyList());
        when(gdacsClient.getGeometryByLink(alert.getEventType(), alert.getEventId(), alert.getCurrentEpisodeId()))
                .thenReturn(geometry);
        when(gdacsDataLakeConverter.convertGdacs(alert))
                .thenReturn(createDataLake(alert, alert.getData(), GDACS_ALERT_PROVIDER));
        when(gdacsDataLakeConverter.convertGdacsWithGeometry(alert, geometry))
                .thenReturn(createDataLake(alert, geometry, GDACS_ALERT_GEOMETRY_PROVIDER));

        List<DataLake> dataLakes = gdacsService.createDataLakeListWithAlertsAndGeometry(
                Map.of("GDACS_EQ_1243255_1342589", alert));

        assertEquals(2, dataLakes.size());
        assertEquals(GDACS_ALERT_PROVIDER, dataLakes.get(0).getProvider());
        assertEquals(GDACS_ALERT_GEOMETRY_PROVIDER, dataLakes.get(1).getProvider());
    }

    @Test
    public void testCreateDataLakeListWithAlertsAndGeometryWhenThrows() throws IOException {
        ParsedAlert alert = getParsedAlert();
        when(dataLakeDao.getDataLakesByExternalId(alert.getIdentifier())).thenReturn(Collections.emptyList());
        when(gdacsClient.getGeometryByLink(alert.getEventType(), alert.getEventId(), alert.getCurrentEpisodeId()))
                .thenThrow(FeignException.class);

        List<DataLake> dataLakes = gdacsService.createDataLakeListWithAlertsAndGeometry(
                Map.of("GDACS_EQ_1243255_1342589", alert));

        assertEquals(0, dataLakes.size());
        verify(gdacsDataLakeConverter, times(0)).convertGdacs(any());
        verify(gdacsDataLakeConverter, times(0)).convertGdacsWithGeometry(any(), anyString());
    }

    private ParsedAlert getParsedAlert() throws IOException {
        return new ParsedAlert(
                OffsetDateTime.parse("Tue, 10 Nov 2020 06:07:49 GMT", DateTimeFormatter.RFC_1123_DATE_TIME),
                "GDACS_EQ_1243255_1342589",
                "1243255",
                "EQ",
                "1342589",
                readFile("alert.xml"));
    }

    private DataLake createDataLake(ParsedAlert alert, String data, String provider) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), alert.getIdentifier(),
                alert.getDateModified(), DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(data);
        dataLake.setProvider(provider);
        return dataLake;
    }

    private String readFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
    }
}