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

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        var hazardsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 1, 1), ZoneOffset.UTC);
        var mags01LoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 1, 2), ZoneOffset.UTC);
        var mags02LoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 3), ZoneOffset.UTC);

        createNormalizations(externalId, hazardsLoadTime, HP_SRV_SEARCH_PROVIDER, readMessageFromFile("hpsrvhazard02.json"));
        var eventOptionalVersion1 = konturEventsDao.getLatestEventByExternalId(externalId);
        assertTrue(eventOptionalVersion1.isPresent());
        assertEquals(1, eventOptionalVersion1.get().getObservationIds().size());

        createNormalizations(externalId, mags01LoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata02.json"));
        eventCombinationJob.run();
        createNormalizations(externalId, mags02LoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata03.json"));
        eventCombinationJob.run();
        var eventOptionalVersion2 = konturEventsDao.getLatestEventByExternalId(externalId);
        assertTrue(eventOptionalVersion2.isPresent());
        assertEquals(2, eventOptionalVersion2.get().getObservationIds().size());

        eventCombinationJob.run();
        var eventOptionalVersion3 = konturEventsDao.getLatestEventByExternalId(externalId);
        assertTrue(eventOptionalVersion3.isPresent());
        assertEquals(3, eventOptionalVersion3.get().getObservationIds().size());
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}