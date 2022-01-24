package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GdacsSearchJobTest {

    private final GdacsAlertXmlParser gdacsAlertXmlParser = mock(GdacsAlertXmlParser.class);
    private final GdacsService gdacsService = mock(GdacsService.class);
    private final DataLakeDao dataLakeDao = mock(DataLakeDao.class);

    @Test
    public void testJob() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        String xml = readMessageFromFile("gdacs_cap.xml");
        Map<String, String> alerts = getAlerts();
        Map<String, ParsedAlert> parsedAlerts = getParsedAlerts(alerts);
        List<DataLake> dataLakes = parsedAlerts.values().stream().map(this::getDataLake).collect(Collectors.toList());

        when(gdacsService.fetchGdacsXml()).thenReturn(Optional.of(xml));
        when(gdacsAlertXmlParser.getPubDate(isA(String.class))).thenReturn(getPubDate());
        when(gdacsAlertXmlParser.getAlerts(isA(String.class))).thenReturn(getAlerts());
        when(gdacsAlertXmlParser.getParsedAlertsToGdacsSearchJob(anyMap())).thenReturn(parsedAlerts);
        when(gdacsService.createDataLakeListWithAlertsAndGeometry(anyMap())).thenReturn(dataLakes);
        doNothing().when(gdacsService).saveGdacs(anyList());

        GdacsSearchJob gdacsSearchJob = new GdacsSearchJob(gdacsService, gdacsAlertXmlParser, dataLakeDao,
                new SimpleMeterRegistry());
        assertDoesNotThrow(gdacsSearchJob::run);

        verify(gdacsService, times(1)).fetchGdacsXml();
        verify(gdacsAlertXmlParser, times(1)).getPubDate(isA(String.class));
        verify(gdacsAlertXmlParser, times(1)).getAlerts(isA(String.class));
        verify(gdacsAlertXmlParser, times(1)).getParsedAlertsToGdacsSearchJob(anyMap());
        verify(gdacsService, times(1)).createDataLakeListWithAlertsAndGeometry(anyMap());
        verify(gdacsService, times(1)).saveGdacs(anyList());

    }

    @Test
    public void testFilterAlertsWhenExists() throws IOException {
        Map<String, String> alerts = getAlerts();
        when(dataLakeDao.getDataLakesByExternalIds(anySet()))
                .thenReturn(Collections.singletonList(
                        new DataLake(UUID.randomUUID(), "GDACS_TC_1000742_1", null, null)));

        GdacsSearchJob gdacsSearchJob = new GdacsSearchJob(gdacsService, gdacsAlertXmlParser, dataLakeDao,
                new SimpleMeterRegistry());
        Map<String, String> filteredAlerts = gdacsSearchJob.filterExistsAlerts(alerts);

        assertEquals(2, filteredAlerts.size());
    }


    private OffsetDateTime getPubDate() {
        return OffsetDateTime.parse("Tue, 10 Nov 2020 05:15 GMT", DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private Map<String, String> getAlerts() throws IOException {
        return Map.of(
                "GDACS_EQ_1243255_1342589", readMessageFromFile("alert01.xml"),
                "GDACS_TC_1000742_1", readMessageFromFile("alert02.xml"),
                "GDACS_EQ_1243234_1342580", readMessageFromFile("alert03.xml")
        );
    }

    private Map<String, ParsedAlert> getParsedAlerts(Map<String, String> alerts) {
        return Map.of(
                "GDACS_EQ_1243255_1342589", new ParsedAlert(OffsetDateTime.parse("Tue, 10 Nov 2020 06:07:49 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME), "GDACS_EQ_1243255_1342589", "1243255",
                        "EQ", "1342589", alerts.get("GDACS_EQ_1243255_1342589")),
                "GDACS_TC_1000742_1", new ParsedAlert(OffsetDateTime.parse("Tue, 10 Nov 2020 05:03:09 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME), "GDACS_TC_1000742_1", "1000742",
                        "TC", "1", alerts.get("GDACS_TC_1000742_1")),
                "GDACS_EQ_1243234_1342580", new ParsedAlert(OffsetDateTime.parse("Tue, 10 Nov 2020 03:12:19 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME), "GDACS_EQ_1243234_1342580", "1243234",
                        "EQ", "1342580", alerts.get("GDACS_EQ_1243234_1342580")));
    }

    private DataLake getDataLake(ParsedAlert parsedAlert) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), parsedAlert.getIdentifier(),
                parsedAlert.getDateModified(), DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(parsedAlert.getData());
        dataLake.setProvider(GDACS_ALERT_PROVIDER);
        dataLake.setNormalized(false);
        return dataLake;
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
    }

}