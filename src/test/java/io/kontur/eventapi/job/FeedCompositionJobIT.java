package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.*;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;


public class FeedCompositionJobIT extends AbstractIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeMapper dataLakeMapper;
    private final FeedMapper feedMapper;
    private final KonturEventsDao konturEventsDao;

    @Autowired
    public FeedCompositionJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob,
                                FeedCompositionJob feedCompositionJob, DataLakeMapper dataLakeMapper,
                                FeedMapper feedMapper, KonturEventsDao konturEventsDao) {
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeMapper = dataLakeMapper;
        this.feedMapper = feedMapper;
        this.konturEventsDao = konturEventsDao;
    }

    @Test
    public void testUpdateDates() throws IOException {
        String eventUUID = "0457178b-45c1-492f-bbc1-61ca14389a31";
        //given
        var hazardsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC);

        //when
        FeedData feedV1 = createFeed(eventUUID, hazardsLoadTime, HP_SRV_SEARCH_PROVIDER,
                readMessageFromFile("hpsrvhazard01.json"));

        //then
        assertEquals(hazardsLoadTime, feedV1.getUpdatedAt());
        assertEquals(hazardsLoadTime, feedV1.getEpisodes().get(0).getUpdatedAt());
        assertEquals(Instant.ofEpochMilli(1594760678798L),
                feedV1.getEpisodes().get(0).getSourceUpdatedAt().toInstant());

        assertFalse(feedV1.getEpisodes().isEmpty());
        assertFalse(feedV1.getEpisodes().get(0).getObservations().isEmpty());

        //given
        var magsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2), ZoneOffset.UTC);

        //when
        FeedData feedV2 = createFeed(eventUUID, magsLoadTime, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata01.json"));

        //then
        assertEquals(magsLoadTime, feedV2.getUpdatedAt());
        assertEquals(hazardsLoadTime, feedV2.getEpisodes().get(0).getUpdatedAt());
        assertEquals(magsLoadTime, feedV2.getEpisodes().get(1).getUpdatedAt());
        assertEquals(Instant.ofEpochMilli(1594760678798L),
                feedV2.getEpisodes().get(0).getSourceUpdatedAt().toInstant());
        assertEquals(OffsetDateTime
                        .parse("2020-07-14T21:05:26.591+0000", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                feedV2.getEpisodes().get(1).getSourceUpdatedAt());
    }

    private FeedData createFeed(String externalEventUUId, OffsetDateTime loadedTime, String provider, String data) {
        createNormalizations(externalEventUUId, loadedTime, provider, data);
        eventCombinationJob.run();
        feedCompositionJob.run();

        return feedMapper.searchForEvents(
                "pdc-v0",
                Collections.emptyList(),
                loadedTime.minusDays(1),
                1
        ).get(0);
    }

    private void createNormalizations(String externalEventUUId, OffsetDateTime loadedTime, String provider, String data){
        var dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(externalEventUUId);
        dataLake.setLoadedAt(loadedTime);
        dataLake.setProvider(provider);
        dataLake.setData(data);

        dataLakeMapper.create(dataLake);
        normalizationJob.run();
    }

    @Test
    public void testSavedMags() throws IOException {
        String externalId = "996d6b0a-ce18-47d9-9bd2-b1b8fe5d967a";

        var pdcFeed = feedMapper.getFeeds()
                .stream()
                .filter(feed -> feed.getAlias().equals("pdc-v0"))
                .findFirst()
                .orElseThrow();

        var hazardsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 1, 1), ZoneOffset.UTC);
        var mags01LoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 1, 2), ZoneOffset.UTC);
        var mags02LoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 3), ZoneOffset.UTC);

        createNormalizations(externalId, hazardsLoadTime, HP_SRV_SEARCH_PROVIDER, readMessageFromFile("hpsrvhazard02.json"));
        eventCombinationJob.run();
        var eventOptionalVersion1 = konturEventsDao.getLatestEventByExternalId(externalId);
        assertTrue(eventOptionalVersion1.isPresent());
        assertEquals(1, eventOptionalVersion1.get().getObservationIds().size());

        createNormalizations(externalId, mags01LoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata02.json"));
        createNormalizations(externalId, mags02LoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata03.json"));
        eventCombinationJob.run();

        var eventList = konturEventsDao.getNewEventVersionsForFeed(pdcFeed.getFeedId())
                .stream()
                .filter(event -> event.getEventId().equals(eventOptionalVersion1.get().getEventId()))
                .collect(toList());

        assertEquals(3, eventList.size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

    @Test
    public void testMagDuplicate() throws IOException {
        String externalId = "0c653cb4-4a9e-4506-b2c8-a1e14e4dc049";
        var startTimeForSearchingFeed = OffsetDateTime.now();

        var hazardLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2024, 10, 1, 1, 1), ZoneOffset.UTC);
        var sqsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2024, 10, 1, 1, 2), ZoneOffset.UTC);
        var magsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2024, 10, 1, 1, 2, 1), ZoneOffset.UTC);

        createNormalizations(externalId, hazardLoadTime, HP_SRV_SEARCH_PROVIDER, readMessageFromFile("hpsrvhazard04.json"));

        eventCombinationJob.run();
        feedCompositionJob.run();

        createNormalizations(externalId, sqsLoadTime, PDC_SQS_PROVIDER, readMessageFromFile("sqsdata04.json"));
        createNormalizations(externalId, magsLoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata04.json"));

        eventCombinationJob.run();
        feedCompositionJob.run();

        FeedData feed = feedMapper.searchForEvents(
                "pdc-v0",
                Collections.emptyList(),
                startTimeForSearchingFeed,
                1
        ).get(0);
        assertEquals(2, feed.getEpisodes().size());

        boolean oneEpisodeForTwoObservation = feed.getEpisodes().stream()
                .anyMatch(episode -> episode.getObservations().size() > 1);
        assertTrue(oneEpisodeForTwoObservation);
    }

}