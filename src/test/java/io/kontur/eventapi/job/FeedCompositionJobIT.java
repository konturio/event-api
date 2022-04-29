package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.entity.OpenFeedData;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.com.google.common.collect.Iterables.getOnlyElement;

public class FeedCompositionJobIT extends AbstractCleanableIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeDao dataLakeDao;
    private final FeedDao feedDao;
    private final NormalizedObservationsDao observationsDao;

    @Autowired
    public FeedCompositionJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, DataLakeDao dataLakeDao, FeedDao feedDao, JdbcTemplate jdbcTemplate, NormalizedObservationsDao observationsDao) {
        super(jdbcTemplate);
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeDao = dataLakeDao;
        this.feedDao = feedDao;
        this.observationsDao = observationsDao;
    }

    @Test
    public void testEpisodesWhenStartDateLaterThenEndedDate() throws IOException {
        //given data_lake with started_at > ended_at
        //when episodes crated
        OpenFeedData feedV1 = createFeed(UUID.randomUUID().toString(), OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC),
                HP_SRV_SEARCH_PROVIDER, readMessageFromFile("hpsrvhazard_with_start_later_then_end.json"));

        //then
        assertEquals(2, feedV1.getEpisodes().size());
        assertEquals(feedV1.getEpisodes().get(1).getName(), feedV1.getEpisodes().get(0).getName());

        NormalizedObservation observation = getOnlyElement(observationsDao.getObservations(feedV1.getObservations()));

        assertEquals(observation.getEndedAt(), feedV1.getEpisodes().get(0).getStartedAt());
        assertEquals(observation.getEndedAt(), feedV1.getEpisodes().get(0).getEndedAt());

        assertEquals(observation.getStartedAt(), feedV1.getEpisodes().get(1).getStartedAt());
        assertEquals(observation.getStartedAt(), feedV1.getEpisodes().get(1).getEndedAt());
    }

    @Test
    public void testUpdateDates() throws IOException {
        String eventUUID = "0457178b-45c1-492f-bbc1-61ca14389a31";
        //given
        var hazardsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC);

        //when
        OpenFeedData feedV1 = createFeed(eventUUID, hazardsLoadTime, HP_SRV_SEARCH_PROVIDER,
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
        OpenFeedData feedV2 = createFeed(eventUUID, magsLoadTime, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata01.json"));

        //then
        assertEquals(magsLoadTime, feedV2.getUpdatedAt());
        assertEquals(hazardsLoadTime, feedV2.getEpisodes().get(0).getUpdatedAt());
        assertEquals(magsLoadTime, feedV2.getEpisodes().get(1).getUpdatedAt());
        assertEquals(Instant.ofEpochMilli(1594760678798L),
                feedV2.getEpisodes().get(0).getSourceUpdatedAt().toInstant());
        assertEquals(OffsetDateTime
                        .parse("2020-07-14T21:07:26.591+0000", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                feedV2.getEpisodes().get(1).getSourceUpdatedAt());
    }

    private OpenFeedData createFeed(String externalEventUUId, OffsetDateTime loadedTime, String provider, String data) {
        createNormalizations(externalEventUUId, loadedTime, provider, data);
        eventCombinationJob.run();
        feedCompositionJob.run();

        return feedDao.searchForEvents("test-pdc-v0", List.of(), null,
                null, null, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY).get(0);
    }

    private void createNormalizations(String externalEventUUId, OffsetDateTime loadedTime, String provider, String data) {
        var dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(externalEventUUId);
        dataLake.setLoadedAt(loadedTime);
        dataLake.setProvider(provider);
        dataLake.setData(data);

        dataLakeDao.storeEventData(dataLake);
        normalizationJob.run();
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

        OpenFeedData feed = feedDao.searchForEvents("test-pdc-v0", List.of(), null, null, startTimeForSearchingFeed,
                1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY).get(0);
        assertEquals(2, feed.getEpisodes().size());

        boolean oneEpisodeForTwoObservation = feed.getEpisodes().stream()
                .anyMatch(episode -> episode.getObservations().size() > 1);
        assertTrue(oneEpisodeForTwoObservation);
    }

    @Test
    public void testOrderNormalization() throws IOException {

        var latestUpdatedDate = OffsetDateTime.of(LocalDateTime.of(2020, 9, 17, 20, 54, 26), ZoneOffset.UTC);

        var sqsDataLake01 = new DataLake();
        sqsDataLake01.setObservationId(UUID.fromString("12ab7440-779b-46c7-8f40-448780cd31e1"));
        sqsDataLake01.setExternalId("6f7c4ea0-a295-5964-ac80-197d6dd73ad7");
        sqsDataLake01.setProvider(PDC_SQS_PROVIDER);
        sqsDataLake01.setLoadedAt(OffsetDateTime.of(LocalDateTime.of(2020, 9, 17, 20, 54, 22), ZoneOffset.UTC));
        sqsDataLake01.setData(readMessageFromFile("SQSDataTestOrder01.json"));
        dataLakeDao.storeEventData(sqsDataLake01);

        var sqsDataLake02 = new DataLake();
        sqsDataLake02.setObservationId(UUID.fromString("39f665ef-4400-4acb-8ccb-5c9c3a2d2346"));
        sqsDataLake02.setExternalId("d6557e52-8a91-56fb-b46c-298f73ff99a3");
        sqsDataLake02.setProvider(PDC_SQS_PROVIDER);
        sqsDataLake02.setLoadedAt(latestUpdatedDate);
        sqsDataLake02.setData(readMessageFromFile("SQSDataTestOrder02.json"));
        dataLakeDao.storeEventData(sqsDataLake02);

        var loadHpSrvHazardLoadTime = OffsetDateTime.of(LocalDateTime.of(2020, 9, 17, 10, 10, 33), ZoneOffset.UTC);
        var hpSrvHazardDataLake = new DataLake();
        hpSrvHazardDataLake.setObservationId(UUID.fromString("40052d98-6380-4b59-84f8-c6e733a64979"));
        hpSrvHazardDataLake.setExternalId("f01fa876-6955-43bf-895c-37dfca330803");
        hpSrvHazardDataLake.setProvider(HP_SRV_SEARCH_PROVIDER);
        hpSrvHazardDataLake.setLoadedAt(loadHpSrvHazardLoadTime);
        hpSrvHazardDataLake.setUpdatedAt(OffsetDateTime.of(LocalDateTime.of(2020, 9, 17, 8, 56, 9), ZoneOffset.UTC));
        hpSrvHazardDataLake.setData(readMessageFromFile("HpSrvSearchTestOrder.json"));
        dataLakeDao.storeEventData(hpSrvHazardDataLake);

        normalizationJob.run();
        eventCombinationJob.run();

        feedCompositionJob.run();

        OpenFeedData feed = feedDao.searchForEvents("test-pdc-v0", List.of(EventType.WILDFIRE), null, null,
                loadHpSrvHazardLoadTime, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY).get(0);
        assertEquals(latestUpdatedDate, feed.getUpdatedAt());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}