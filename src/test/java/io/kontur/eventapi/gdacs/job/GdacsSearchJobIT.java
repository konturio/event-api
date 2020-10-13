package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GdacsSearchJobIT extends AbstractIntegrationTest {

    private final GdacsSearchJob gdacsSearchJob;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public GdacsSearchJobIT(GdacsSearchJob gdacsSearchJob, DataLakeDao dataLakeDao) {
        this.gdacsSearchJob = gdacsSearchJob;
        this.dataLakeDao = dataLakeDao;
    }

    @Test
    public void testNumberOfItems() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        String xml = readMessageFromFile("gdacs.xml");
        int itemsCount = 65;
        assertEquals(itemsCount, gdacsSearchJob.getLinks(xml).size());
    }

    @Test
    public void testLinks(){
        var listOfLinks = List.of(
                "/contentdata/resources/EQ/1239039/cap_1239039.xml",
                "dsfsfsrf",
                "",
                "               ",
                "/contentdata/resources/EQ/1239035/cap_1239035.xml",
                "/contentdata/resources/EQ/1239035/cap_9999999.xml",
                "/"
        );
        int alertCount = 2;
        assertEquals(alertCount, gdacsSearchJob.getAlerts(listOfLinks).size());
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
        assertEquals(alertCount, gdacsSearchJob.getAlertsForDateLake(listOfAlerts).size());
    }

    @Test
    public void testSaveAlerts(){
        var dateModified = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1),
                ZoneOffset.UTC
        );
        String externalId = "GDACS_EQ_1239039_1337379";
        String data = "<alert></alert>";
        int expectedListSize = 1;

        gdacsSearchJob.saveAlerts(List.of(new AlertForInsertDataLake(
                dateModified,
                externalId,
                data
        )));

        gdacsSearchJob.saveAlerts(List.of(new AlertForInsertDataLake(
                dateModified,
                externalId,
                data
        )));
        var dataLakesAfterFirstUpdate = dataLakeDao.getDataLakeByExternalIdAndUpdateDate(externalId, dateModified);
        assertEquals(expectedListSize, dataLakesAfterFirstUpdate.size());

        var datePlusDay = dateModified.plusDays(1);
        gdacsSearchJob.saveAlerts(List.of(new AlertForInsertDataLake(
                datePlusDay,
                externalId,
                data
        )));
        var dataLakesAfterSecondUpdate = dataLakeDao.getDataLakeByExternalIdAndUpdateDate(externalId, datePlusDay);
        assertEquals(expectedListSize, dataLakesAfterSecondUpdate.size());

        var dateMinusDay = dateModified.minusDays(1);
        gdacsSearchJob.saveAlerts(List.of(new AlertForInsertDataLake(
                dateMinusDay,
                externalId,
                data
        )));
        var dataLakesAfterThirdUpdate = dataLakeDao.getDataLakeByExternalIdAndUpdateDate(externalId, dateMinusDay);
        assertEquals(expectedListSize, dataLakesAfterThirdUpdate.size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}