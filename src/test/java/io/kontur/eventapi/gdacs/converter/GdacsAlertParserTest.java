package io.kontur.eventapi.gdacs.converter;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GdacsAlertParserTest {

    @Test
    public void testNumberOfItems() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        String xml = readMessageFromFile("gdacs.xml");
        int itemsCount = 65;
        assertEquals(itemsCount, new GdacsAlertParser().getLinks(xml).size());
    }

        @Test
    public void testAlerts() throws IOException, ParserConfigurationException, XPathExpressionException {
        var listOfAlerts = List.of(
                readMessageFromFile("alert01_valid.xml"),
                readMessageFromFile("alert02_without_identifier.xml"),
                readMessageFromFile("alert02_without_parameters.xml"),
                readMessageFromFile("alert02_invaliddate.xml"),
                readMessageFromFile("alert02_valid.xml")
        );

        int alertCount = 2;
        assertEquals(alertCount, new GdacsAlertParser().getParsedAlertsToGdacsSearchJob(listOfAlerts).size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}