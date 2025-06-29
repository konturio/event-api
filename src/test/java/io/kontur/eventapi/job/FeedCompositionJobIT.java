package io.kontur.eventapi.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.GeometryFilterType;
import io.kontur.eventapi.resource.dto.TestEventDto;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.resource.dto.TestEventListDto;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.com.google.common.collect.Iterables.getOnlyElement;

public class FeedCompositionJobIT extends AbstractCleanableIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeDao dataLakeDao;
    private final NormalizedObservationsDao observationsDao;
    private final ApiDao apiDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public FeedCompositionJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, DataLakeDao dataLakeDao, FeedDao feedDao, JdbcTemplate jdbcTemplate, NormalizedObservationsDao observationsDao, ApiDao apiDao) {
        super(jdbcTemplate, feedDao);
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeDao = dataLakeDao;
        this.observationsDao = observationsDao;
        this.apiDao = apiDao;
    }

    @PostConstruct
    public void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testEpisodesWhenStartDateLaterThenEndedDate() throws IOException {
        //given data_lake with started_at > ended_at
        //when episodes crated
        TestEventDto feedV1 = createFeed(UUID.randomUUID().toString(), OffsetDateTime.of(
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
        TestEventDto feedV1 = createFeed(eventUUID, hazardsLoadTime, HP_SRV_SEARCH_PROVIDER,
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
        TestEventDto feedV2 = createFeed(eventUUID, magsLoadTime, HP_SRV_MAG_PROVIDER,
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

    private TestEventDto createFeed(String externalEventUUId, OffsetDateTime loadedTime, String provider, String data) throws IOException {
        createNormalizations(externalEventUUId, loadedTime, provider, data);
        eventCombinationJob.run();
        feedCompositionJob.run();

        return objectMapper.readValue(apiDao.searchForEvents("test-feed", List.of(), null,
                null, null, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY, GeometryFilterType.ANY), TestEventListDto.class).getData().get(0);
    }

    private void createNormalizations(String externalEventUUId, OffsetDateTime loadedTime, String provider, String data) {
        var dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(externalEventUUId);
        dataLake.setLoadedAt(loadedTime);
        dataLake.setProvider(provider);
        dataLake.setData(data);

        dataLakeDao.storeEventData(dataLake);
        normalizationJob.run(List.of(provider));
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

        normalizationJob.run(List.of(HP_SRV_SEARCH_PROVIDER));
        eventCombinationJob.run();

        feedCompositionJob.run();

        TestEventDto feed = objectMapper.readValue(apiDao.searchForEvents(
                "test-feed", List.of(EventType.FLOOD), null, null, loadHpSrvHazardLoadTime, 1,
                List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY, GeometryFilterType.ANY), TestEventListDto.class).getData().get(0);
        assertEquals(latestUpdatedDate, feed.getUpdatedAt());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}