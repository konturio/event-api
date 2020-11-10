package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.resource.dto.EventDto;
import io.kontur.eventapi.service.EventResourceService;
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

import static org.junit.jupiter.api.Assertions.*;

public class GdacsSearchJobIT extends AbstractIntegrationTest {

    private final GdacsSearchJob gdacsSearchJob;
    private final DataLakeDao dataLakeDao;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final EventResourceService eventResourceService;

    @Autowired
    public GdacsSearchJobIT(GdacsSearchJob gdacsSearchJob, DataLakeDao dataLakeDao, NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, EventResourceService eventResourceService) {
        this.gdacsSearchJob = gdacsSearchJob;
        this.dataLakeDao = dataLakeDao;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.eventResourceService = eventResourceService;
    }

    @Test
    public void testNumberOfItems() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        String xml = readMessageFromFile("gdacs.xml");
        int itemsCount = 65;
        assertEquals(itemsCount, gdacsSearchJob.getLinksAndPubDate(xml).size());
    }

    @Test
    public void testLinks(){
        var listOfLinks = List.of(
                "/contentdata/resources/EQ/1239039/cap_1239039.xml",
                "dsfsfsrf",
                "/contentdata/resources/EQ/1239035/cap_1239035.xml",
                "/contentdata/resources/EQ/1239035/cap_9999999.xml"
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
        assertEquals(alertCount, gdacsSearchJob.getSortedBySentAlertsForDataLake(listOfAlerts).size());
    }

    @Test
    public void testSaveAlerts(){
        var dateModified = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1),
                ZoneOffset.UTC
        );
        String externalId = "EQ_1239039";
        String data = "<alert></alert>";
        int expectedListSize = 1;

        gdacsSearchJob.saveAlerts(List.of(new AlertForInsertDataLake(
                dateModified,
                externalId,
                data,
                dateModified.plusSeconds(1)
        )));

        gdacsSearchJob.saveAlerts(List.of(new AlertForInsertDataLake(
                dateModified,
                externalId,
                data,
                dateModified.plusSeconds(2)
        )));

        assertEquals(expectedListSize, dataLakeDao.getDataLakesByExternalId(externalId).size());
    }

    @Test
    public void testSortingAlertsAndEventBySentParameter() throws IOException, XPathExpressionException, ParserConfigurationException {
        String alert01 = readMessageFromFile("alert_for_test_sorting_by_sent_v1.xml");
        String id01 = "GDACS_TC_1000738_16";
        String alert02 = readMessageFromFile("alert_for_test_sorting_by_sent_v2.xml");
        String id02 = "GDACS_TC_1000738_19";

        var alerts = List.of(alert01, alert02);
        var alertsForDataLake = gdacsSearchJob.getSortedBySentAlertsForDataLake(alerts);

        assertFalse(alertsForDataLake.isEmpty());

//        second alert was sending earlier
        assertEquals(id02, alertsForDataLake.get(0).getExternalId());
        assertEquals(id01, alertsForDataLake.get(1).getExternalId());

        gdacsSearchJob.saveAlerts(alertsForDataLake);
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        List<EventDto> events = eventResourceService.searchEvents("gdacs", List.of(), OffsetDateTime.now().minusYears(10), 1, List.of(), SortOrder.ASC);
        String expectedDescriptionOfLatestEpisode = "From 31/10/2020 to 04/11/2020, a Tropical Storm (maximum wind speed of 241 km/h) ETA-20 was active in Atlantic. The cyclone affects these countries: Nicaragua, Honduras (vulnerability Medium). Estimated population affected by category 1 (120 km/h) wind speeds or higher is 0.126 million.";
        String actualDescriptionOfLatestEpisode = events.get(0).getEpisodes().get(events.get(0).getEpisodes().size() - 1).getDescription();

        assertFalse(events.isEmpty());
        assertEquals(expectedDescriptionOfLatestEpisode, actualDescriptionOfLatestEpisode);

    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}