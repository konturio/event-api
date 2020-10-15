package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.dao.mapper.NormalizedObservationsMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeedCompositionJobIT extends AbstractIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeMapper dataLakeMapper;
    private final FeedMapper feedMapper;
    private final NormalizedObservationsMapper normalizedObservationsMapper;

    @Autowired
    public FeedCompositionJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, DataLakeMapper dataLakeMapper, FeedMapper feedMapper, NormalizedObservationsMapper normalizedObservationsMapper) {
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeMapper = dataLakeMapper;
        this.feedMapper = feedMapper;
        this.normalizedObservationsMapper = normalizedObservationsMapper;
    }

    @Test
    public void testUpdateDates() throws IOException {
        String hazardsProvider = "hpSrvSearch";
        String magsProvider = "hpSrvMag";
        String externalEventUUId01 = "0457178b-45c1-492f-bbc1-61ca14389a31";
        String externalEventUUId = "01ee0b34-d7e2-479e-a46f-0f68f181a66e";

        String hazardData01 = readMessageFromFile("hpsrvhazard01.json");
        var hazardsLoadTime01 = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1),
                ZoneOffset.UTC
        );
        var hazardsObservationId01 = UUID.fromString("8ac2acda-0ef7-4976-876b-580b82c29bea");

        String hazardData = readMessageFromFile("hpsrvhazard02.json");
        var hazardsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 4, 25, 2, 31, 25),
                ZoneOffset.UTC
        );
        var hazardsObservationId = UUID.fromString("fa104aaf-60fb-4544-9b4d-7015e7dedc1d");

        FeedData hazardFeed01 = createFeed(externalEventUUId01, hazardsObservationId01, hazardsLoadTime01, hazardsProvider, hazardData01);
        FeedData hazardFeed02 = createFeed(externalEventUUId, hazardsObservationId, hazardsLoadTime, hazardsProvider, hazardData);

        String magsData01 = readMessageFromFile("magsdata01.json");
        var magsLoadTime01 = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2),
                ZoneOffset.UTC
        );
        var magsObservationId01 = UUID.fromString("10248c2c-c22f-4c35-a7ed-c9b36646ced4");

        String magsData02 = readMessageFromFile("magsdata02.json");
        var magsLoadTime02 = OffsetDateTime.of(
                LocalDateTime.of(2020, 9, 30, 10, 32, 46),
                ZoneOffset.UTC
        );
        var magsObservationId02 = UUID.fromString("00600d61-81e0-42d0-a4d6-fd42971d8906");

        FeedData magsFeed01 = createFeed(externalEventUUId01, magsObservationId01, magsLoadTime01, magsProvider, magsData01);
        FeedEpisode episode01 = magsFeed01.getEpisodes().get(0);

        FeedData magsFeed02 = createFeed(externalEventUUId, magsObservationId02, magsLoadTime02, magsProvider, magsData02);

        NormalizedObservation magNormalizedObservation01 = normalizedObservationsMapper.getObservationsByExternalId(externalEventUUId01)
                .stream()
                .filter(obs -> obs.getProvider().equals(magsProvider))
                .findFirst()
                .orElseThrow();

        assertTrue(hazardsLoadTime01.isEqual(hazardFeed01.getUpdatedAt()));
        assertTrue(magsLoadTime01.isEqual(magsFeed01.getUpdatedAt()));
        assertTrue(magsFeed01.getUpdatedAt().isAfter(hazardFeed01.getUpdatedAt()));

        assertTrue(episode01.getUpdatedAt().isEqual(magsLoadTime01));
        assertTrue(episode01.getSourceUpdatedAt().isEqual(magNormalizedObservation01.getSourceUpdatedAt()));

        assertFalse(hazardFeed02.getUpdatedAt().isEqual(magsFeed02.getUpdatedAt()));
    }

    private FeedData createFeed(String externalEventUUId, UUID observationID, OffsetDateTime loadedTime, String provider, String data) {
        var dataLake = new DataLake();
        dataLake.setObservationId(observationID);
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
                loadedTime.minusDays(1),
                0,
                1
        ).get(0);
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }


}