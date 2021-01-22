package io.kontur.eventapi.gdacs.converter;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GdacsAlertXmlParserTest {

    @Test
    public void testNumberOfItems() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        String xml = readMessageFromFile("gdacs.xml");
        int itemsCount = 65;
        assertEquals(itemsCount, new GdacsAlertXmlParser().getLinks(xml).size());
    }

    @Test
    public void testAlerts() throws IOException, ParserConfigurationException, XPathExpressionException {
        var listOfAlerts = Map.of(
                "1", readMessageFromFile("alert01_valid.xml"),
                "2", readMessageFromFile("alert02_without_identifier.xml"),
                "3", readMessageFromFile("alert02_without_parameters.xml"),
                "4", readMessageFromFile("alert02_invaliddate.xml"),
                "5", readMessageFromFile("alert02_valid.xml")
        );

        int alertCount = 2;
        assertEquals(alertCount, new GdacsAlertXmlParser().getParsedAlertsToGdacsSearchJob(listOfAlerts).size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}