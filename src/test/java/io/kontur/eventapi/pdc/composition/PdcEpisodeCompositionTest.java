package io.kontur.eventapi.pdc.composition;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
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

public class PdcEpisodeCompositionTest extends AbstractCleanableIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeDao dataLakeDao;
    private final ApiDao apiDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PdcEpisodeCompositionTest(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob,
                                     FeedCompositionJob feedCompositionJob, DataLakeDao dataLakeDao, FeedDao feedDao,
                                     JdbcTemplate jdbcTemplate, ApiDao apiDao) {
        super(jdbcTemplate, feedDao);
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeDao = dataLakeDao;
        this.apiDao = apiDao;
    }

    @PostConstruct
    public void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testOneEpisodeFromTwoObservations() throws IOException {
        String externalId = "0457178b-45c1-492f-bbc1-61ca14389a31";

        var hazardLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC);
        var magsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2), ZoneOffset.UTC);

        createNormalizations(externalId, hazardLoadTime, HP_SRV_SEARCH_PROVIDER,
                readMessageFromFile("hpsrvhazard1.json"));
        createNormalizations(externalId, magsLoadTime, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata1.json"));

        eventCombinationJob.run();
        feedCompositionJob.run();

        TestEventDto feed = objectMapper.readValue(apiDao.searchForEvents("test-feed", List.of(), null, null,
                null, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY, GeometryFilterType.ANY), TestEventListDto.class).getData().get(0);
        assertNotNull(feed.getEpisodes());
        assertEquals(1, feed.getEpisodes().size());
        assertNotNull(feed.getEpisodes().get(0).getObservations());
        assertEquals(2, feed.getEpisodes().get(0).getObservations().size());
        assertEquals(magsLoadTime, feed.getEpisodes().get(0).getUpdatedAt());
        assertEquals(OffsetDateTime.parse("2020-07-14T21:05:26.591+0000",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                feed.getEpisodes().get(0).getSourceUpdatedAt());
        assertNotNull(feed.getEpisodes().get(0).getGeometries());
        assertEquals(2, feed.getEpisodes().get(0).getGeometries().getFeatures().length);
    }

    @Test
    public void testOneEpisodeFromThreeObservations() throws IOException {
        String externalId = "0457178b-45c1-492f-bbc1-61ca14389a31";

        var hazardLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC);
        var magsLoadTime1 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2), ZoneOffset.UTC);
        var magsLoadTime2 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 3), ZoneOffset.UTC);

        createNormalizations(externalId, hazardLoadTime, HP_SRV_SEARCH_PROVIDER,
                readMessageFromFile("hpsrvhazard1.json"));
        createNormalizations(externalId, magsLoadTime1, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata1.json"));
        createNormalizations(externalId, magsLoadTime2, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata2.json"));

        eventCombinationJob.run();
        feedCompositionJob.run();

        TestEventDto feed = objectMapper.readValue(apiDao.searchForEvents("test-feed", List.of(), null, null,
                null, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY, GeometryFilterType.ANY), TestEventListDto.class).getData().get(0);
        assertNotNull(feed.getEpisodes());
        assertEquals(1, feed.getEpisodes().size());
        assertNotNull(feed.getEpisodes().get(0).getObservations());
        assertEquals(3, feed.getEpisodes().get(0).getObservations().size());
        assertEquals(magsLoadTime2, feed.getEpisodes().get(0).getUpdatedAt());
        assertEquals(OffsetDateTime.parse("2020-07-14T21:05:36.591+0000",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                feed.getEpisodes().get(0).getSourceUpdatedAt());
        assertNotNull(feed.getEpisodes().get(0).getGeometries());
        assertEquals(2, feed.getEpisodes().get(0).getGeometries().getFeatures().length);
    }

    @Test
    public void testOneEpisodeFromChainOfObservations() throws IOException {
        String externalId = "0457178b-45c1-492f-bbc1-61ca14389a31";

        var hazardLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC);
        var magsLoadTime1 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2), ZoneOffset.UTC);
        var magsLoadTime2 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 3), ZoneOffset.UTC);
        var magsLoadTime3 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 4), ZoneOffset.UTC);

        createNormalizations(externalId, hazardLoadTime, HP_SRV_SEARCH_PROVIDER,
                readMessageFromFile("hpsrvhazard1.json"));
        createNormalizations(externalId, magsLoadTime1, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata1.json"));
        createNormalizations(externalId, magsLoadTime2, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata2.json"));
        createNormalizations(externalId, magsLoadTime3, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata3.json"));

        eventCombinationJob.run();
        feedCompositionJob.run();

        TestEventDto feed = objectMapper.readValue(apiDao.searchForEvents("test-feed", List.of(), null, null,
                null, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY, GeometryFilterType.ANY), TestEventListDto.class).getData().get(0);
        assertNotNull(feed.getEpisodes());
        assertEquals(1, feed.getEpisodes().size());
        assertNotNull(feed.getEpisodes().get(0).getObservations());
        assertEquals(4, feed.getEpisodes().get(0).getObservations().size());
        assertEquals(magsLoadTime3, feed.getEpisodes().get(0).getUpdatedAt());
        assertEquals(OffsetDateTime.parse("2020-07-14T21:07:00.591+0000",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                feed.getEpisodes().get(0).getSourceUpdatedAt());
        assertNotNull(feed.getEpisodes().get(0).getGeometries());
        assertEquals(2, feed.getEpisodes().get(0).getGeometries().getFeatures().length);
    }

    @Test
    public void testTwoEpisodeFromChainOfObservations() throws IOException {
        String externalId = "0457178b-45c1-492f-bbc1-61ca14389a31";

        var magsLoadTime1 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2), ZoneOffset.UTC);
        var magsLoadTime2 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 3), ZoneOffset.UTC);
        var magsLoadTime3 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 4), ZoneOffset.UTC);
        var magsLoadTime4 = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1), ZoneOffset.UTC);

        createNormalizations(externalId, magsLoadTime4, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata4.json"));
        createNormalizations(externalId, magsLoadTime1, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata1.json"));
        createNormalizations(externalId, magsLoadTime2, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata2.json"));
        createNormalizations(externalId, magsLoadTime3, HP_SRV_MAG_PROVIDER,
                readMessageFromFile("magsdata3.json"));

        eventCombinationJob.run();
        feedCompositionJob.run();

        TestEventDto feed = objectMapper.readValue(apiDao.searchForEvents("test-feed", List.of(), null, null,
                null, 1, List.of(), SortOrder.ASC, null, EpisodeFilterType.ANY, GeometryFilterType.ANY), TestEventListDto.class).getData().get(0);
        assertNotNull(feed.getEpisodes());
        assertEquals(2, feed.getEpisodes().size());
        assertNotNull(feed.getEpisodes().get(1).getObservations());
        assertEquals(3, feed.getEpisodes().get(1).getObservations().size());
        assertEquals(magsLoadTime3, feed.getEpisodes().get(1).getUpdatedAt());
        assertEquals(OffsetDateTime.parse("2020-07-14T21:07:00.591+0000",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")),
                feed.getEpisodes().get(1).getSourceUpdatedAt());
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

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)));
    }

}