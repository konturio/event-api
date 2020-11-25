package io.kontur.eventapi.firms.event;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.kontur.eventapi.firms.jobs.FirmsImportJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static io.kontur.eventapi.TestUtil.readFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FirmsEventAndEpisodeCombinationsJobIT extends AbstractIntegrationTest {
    private final FirmsImportJob firmsImportJob;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final FeedMapper feedMapper;
    private final KonturEventsDao konturEventsDao;

    @MockBean
    private FirmsClient firmsClient;

    @Autowired
    public FirmsEventAndEpisodeCombinationsJobIT(FirmsImportJob firmsImportJob, NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, FeedMapper feedMapper, KonturEventsDao konturEventsDao) {
        this.firmsImportJob = firmsImportJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.feedMapper = feedMapper;
        this.konturEventsDao = konturEventsDao;
    }

    @Test
    @Sql(executionPhase= Sql.ExecutionPhase.BEFORE_TEST_METHOD,scripts="classpath:/io/kontur/eventapi/firms/event/clean.sql")
    public void testFirmsEventAndEpisodesRollout() throws IOException {
        //GIVEN 3 observation within 1 km, 2 of them have same date. And 1 other observation
        Mockito.when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6.csv"));
        Mockito.when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        Mockito.when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));

        //WHEN run event job
        firmsImportJob.run();
        normalizationJob.run();
        eventCombinationJob.run();

        //THEN
        var firmsFeed = feedMapper.getFeeds().stream().filter(feed -> feed.getAlias().equals("firms")).findFirst().orElseThrow();

        List<KonturEvent> eventsForRolloutEpisodes = konturEventsDao.getEventsForRolloutEpisodes(firmsFeed.getFeedId());
        eventsForRolloutEpisodes.sort(Comparator.comparing(e -> e.getObservationIds().size()));

        assertEquals(2, eventsForRolloutEpisodes.size());//2 group of observations with are far from each other (> 1km)
        assertEquals(1, eventsForRolloutEpisodes.get(0).getObservationIds().size());
        assertEquals(3, eventsForRolloutEpisodes.get(1).getObservationIds().size());

        //WHEN run feed job
        feedCompositionJob.run();
        List<FeedData> feedData = searchFeedData();

        //THEN
        assertEquals(2, feedData.size());

        assertEquals(1, feedData.get(0).getObservations().size());
        assertEquals(1, feedData.get(0).getEpisodes().size());

        assertEquals(3, feedData.get(1).getObservations().size());//3 observations within 1 km
        assertEquals(2, feedData.get(1).getEpisodes().size());//2 observations have same date

        assertTrue(feedData.get(1).getEpisodes().get(0).getName().contains("Burnt area 0.0000857, Burning time 3h"));

        //WHEN new data available for modis - 2 observations within 1 km to 2 existing observation
        //and 1 other observation
        Mockito.when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6-update.csv"));
        Mockito.when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        Mockito.when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));

        firmsImportJob.run();
        normalizationJob.run();
        eventCombinationJob.run();

        //THEN
        List<KonturEvent> eventsForRolloutEpisodesUpdated = konturEventsDao.getEventsForRolloutEpisodes(firmsFeed.getFeedId());
        eventsForRolloutEpisodesUpdated.sort(Comparator.comparing(konturEvent -> konturEvent.getObservationIds().size()));

        assertEquals(3, eventsForRolloutEpisodesUpdated.size());//3 group of observations with are far from each other (> 1km)
        assertEquals(1, eventsForRolloutEpisodesUpdated.get(0).getObservationIds().size());
        assertEquals(2, eventsForRolloutEpisodesUpdated.get(1).getObservationIds().size());
        assertEquals(4, eventsForRolloutEpisodesUpdated.get(2).getObservationIds().size());

        //WHEN run feed job again
        feedCompositionJob.run();

        //THEN
        List<FeedData> firmsUpdated = searchFeedData();

        assertEquals(3, firmsUpdated.size());

        assertEquals(1, firmsUpdated.get(0).getObservations().size());
        assertEquals(1, firmsUpdated.get(0).getEpisodes().size());
        assertEquals(1, firmsUpdated.get(0).getVersion());

        assertEquals(2, firmsUpdated.get(1).getObservations().size());
        assertEquals(2, firmsUpdated.get(1).getEpisodes().size());
        assertEquals(2, firmsUpdated.get(1).getVersion());

        assertEquals(4, firmsUpdated.get(2).getObservations().size());
        assertEquals(3, firmsUpdated.get(2).getEpisodes().size());
        assertEquals(2, firmsUpdated.get(2).getVersion());

        assertTrue(firmsUpdated.get(2).getEpisodes().get(0).getName().contains("Burnt area 0.0001715, Burning time 6h"));
    }

    private List<FeedData> searchFeedData() {
        List<FeedData> firms = feedMapper.searchForEvents(
                "firms",
                List.of(EventType.WILDFIRE),
                OffsetDateTime.parse("2020-11-02T11:00Z"),
                100,
                List.of(),
                SortOrder.ASC
        );
        firms.sort(Comparator.comparing(f -> f.getObservations().size()));
        return firms;
    }

    private String readCsv(String fileName) throws IOException {
        return readFile(this,fileName);
    }

}