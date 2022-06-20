package io.kontur.eventapi.gdacs.converter;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GdacsAlertXmlParserTest {

    @Test
    public void testNumberOfItems() throws ParserConfigurationException, SAXException, IOException {
        String xml = readMessageFromFile("gdacs.xml");
        int itemsCount = 64;
        assertEquals(itemsCount, new GdacsAlertXmlParser().getItems(xml).size());
    }

    @Test
    public void testAlerts() throws IOException {
        Map<String, String> listOfAlerts = Map.of(
                "GDACS_EQ_1279931_1389192", readMessageFromFile("alert01_valid.xml"),
                "", readMessageFromFile("alert02_without_identifier.xml"),
                "GDACS_EQ_1278636_1387465", readMessageFromFile("alert02_without_parameters.xml"),
                "GDACS_EQ_1278647_1387486", readMessageFromFile("alert02_invaliddate.xml"),
                "GDACS_EQ_1278646_1387477", readMessageFromFile("alert02_valid.xml")
        );

        int alertCount = 2;
        assertEquals(alertCount, new GdacsAlertXmlParser().getParsedItems(listOfAlerts, "provider").size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
    }

}