package io.kontur.eventapi.gdacs.job;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GdacsSearchJobTest {

    private final GdacsAlertXmlParser gdacsAlertXmlParser = mock(GdacsAlertXmlParser.class);
    private final GdacsService gdacsService = mock(GdacsService.class);

    @Test
    public void testJob() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        String xml = readMessageFromFile("gdacs_cap.xml");
        Map<String, String> alerts = getAlerts();
        List<ParsedAlert> parsedAlerts = getParsedAlerts(alerts);
        List<DataLake> dataLakes = parsedAlerts.stream().map(this::getDataLake).collect(Collectors.toList());

        when(gdacsService.fetchGdacsXml()).thenReturn(Optional.of(xml));
        when(gdacsAlertXmlParser.getPubDate(isA(String.class))).thenReturn(getPubDate());
        when(gdacsAlertXmlParser.getLinks(isA(String.class))).thenReturn(getLinks());
        when(gdacsService.fetchAlerts(anyList())).thenReturn(alerts);
        when(gdacsAlertXmlParser.getParsedAlertsToGdacsSearchJob(anyMap())).thenReturn(parsedAlerts);
        when(gdacsService.createDataLakeListWithAlertsAndGeometry(anyList())).thenReturn(dataLakes);
        doNothing().when(gdacsService).saveGdacs(anyList());

        GdacsSearchJob gdacsSearchJob = new GdacsSearchJob(gdacsService, gdacsAlertXmlParser, new SimpleMeterRegistry());
        assertDoesNotThrow(gdacsSearchJob::run);

        verify(gdacsService, times(1)).fetchGdacsXml();
        verify(gdacsAlertXmlParser, times(1)).getPubDate(isA(String.class));
        verify(gdacsAlertXmlParser, times(1)).getLinks(isA(String.class));
        verify(gdacsService, times(1)).fetchAlerts(anyList());
        verify(gdacsAlertXmlParser, times(1)).getParsedAlertsToGdacsSearchJob(anyMap());
        verify(gdacsService, times(1)).createDataLakeListWithAlertsAndGeometry(anyList());
        verify(gdacsService, times(1)).saveGdacs(anyList());

    }

    private OffsetDateTime getPubDate() {
        return OffsetDateTime.parse("Tue, 10 Nov 2020 05:15 GMT", DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private List<String> getLinks() {
        return List.of("https://www.gdacs.org/contentdata/resources/EQ/1243255/cap_1243255.xml",
                "https://www.gdacs.org/contentdata/resources/TC/1000742/cap_1000742.xml",
                "https://www.gdacs.org/contentdata/resources/EQ/1243234/cap_1243234.xml");
    }

    private Map<String, String> getAlerts() throws IOException {
        return Map.of(
                "1", readMessageFromFile("alert01.xml"),
                "2", readMessageFromFile("alert02.xml"),
                "3", readMessageFromFile("alert03.xml")
        );
    }

    private List<ParsedAlert> getParsedAlerts(Map<String, String> alerts) {
        return List.of(
                new ParsedAlert(OffsetDateTime.parse("Tue, 10 Nov 2020 06:07:49 GMT", DateTimeFormatter.RFC_1123_DATE_TIME),
                        OffsetDateTime.parse("2020-11-10T03:41:51-00:00"), "GDACS_EQ_1243255_1342589",
                        "1243255", "EQ", "1342589", alerts.get("1")),
                new ParsedAlert(
                        OffsetDateTime.parse("Tue, 10 Nov 2020 05:03:09 GMT", DateTimeFormatter.RFC_1123_DATE_TIME),
                        OffsetDateTime.parse("2020-11-10T02:00:00-00:00"), "GDACS_TC_1000742_1",
                        "1000742", "TC", "1", alerts.get("2")),
                new ParsedAlert(
                        OffsetDateTime.parse("Tue, 10 Nov 2020 03:12:19 GMT", DateTimeFormatter.RFC_1123_DATE_TIME),
                        OffsetDateTime.parse("2020-11-10T00:51:54-00:00"), "GDACS_EQ_1243234_1342580",
                        "1243234", "EQ", "1342580", alerts.get("3")));
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
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}