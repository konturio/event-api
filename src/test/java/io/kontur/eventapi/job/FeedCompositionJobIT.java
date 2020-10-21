package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.SortOrder;
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

public class FeedCompositionJobIT extends AbstractIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeMapper dataLakeMapper;
    private final FeedMapper feedMapper;

    @Autowired
    public FeedCompositionJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob,
                                FeedCompositionJob feedCompositionJob, DataLakeMapper dataLakeMapper,
                                FeedMapper feedMapper) {
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeMapper = dataLakeMapper;
        this.feedMapper = feedMapper;
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
        var dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(externalEventUUId);
        dataLake.setLoadedAt(loadedTime);
        dataLake.setProvider(provider);
        dataLake.setData(data);

        dataLakeMapper.create(dataLake);
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        return feedMapper.searchForEvents(
                "pdc-v0",
                Collections.emptyList(),
                loadedTime.minusDays(1),
                1,
                Collections.emptyList(),
                SortOrder.ASC
        ).get(0);
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}