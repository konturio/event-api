package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GdacsSearchJobIT extends AbstractCleanableIntegrationTest {

    private final GdacsSearchJob gdacsSearchJob;
    private final GdacsAlertXmlParser gdacsAlertXmlParser;
    private final GdacsService gdacsService;

    @Autowired
    public GdacsSearchJobIT(GdacsSearchJob gdacsSearchJob, GdacsAlertXmlParser gdacsAlertXmlParser, GdacsService gdacsService, JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.gdacsSearchJob = gdacsSearchJob;
        this.gdacsAlertXmlParser = gdacsAlertXmlParser;
        this.gdacsService = gdacsService;
    }

    @Test
    public void testJob() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        String xml = readMessageFromFile("gdacs_cap.xml");

        var dateTime = gdacsAlertXmlParser.getPubDate(xml);
        assertNotNull(dateTime);

        var links = gdacsSearchJob.getLinks(xml);
        assertEquals(3, links.size());
        assertNotNull(links);
        assertFalse(links.isEmpty());

        var alerts = getAlerts();
        var parsedAlerts = gdacsSearchJob.getSortedParsedAlerts(alerts);

        assertEquals(3, parsedAlerts.size());
        var parsedAlertLast = parsedAlerts.get(parsedAlerts.size() - 1);
        var parsedAlertBeforeLast = parsedAlerts.get(parsedAlerts.size() - 2);
        assertTrue(parsedAlertLast.getSent().isAfter(parsedAlertBeforeLast.getSent()));

        var dataLakes = gdacsService.createDataLakeListWithAlertsAndGeometry(parsedAlerts);
        assertEquals(6, dataLakes.size());

        dataLakes.forEach(dataLake -> {
            assertNotNull(dataLake.getData());
            assertFalse(dataLake.getData().isBlank());
        });
    }

    private List<String> getAlerts() throws IOException {
        return List.of(
                readMessageFromFile("alert01.xml"),
                readMessageFromFile("alert02.xml"),
                readMessageFromFile("alert03.xml")
        );
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}