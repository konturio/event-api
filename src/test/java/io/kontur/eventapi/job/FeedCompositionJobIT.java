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
        String externalEventUUId = "0457178b-45c1-492f-bbc1-61ca14389a31";

        String hazardData = readMessageFromFile("hpsrvhazard.json");
        String hazardsProvider = "hpSrvSearch";
        var hazardasLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 2, 2, 2, 2),
                ZoneOffset.UTC
        );
        var hazardsObservationId = UUID.fromString("8ac2acda-0ef7-4976-876b-580b82c29bea");

        FeedData firstFeed = createFeed(externalEventUUId, hazardsObservationId, hazardasLoadTime, hazardsProvider, hazardData);

        String magsData = readMessageFromFile("magsdata.json");
        String magsProvider = "hpSrvMag";
        var magsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 1, 1, 1, 1),
                ZoneOffset.UTC
        );
        var magsObservationId = UUID.fromString("10248c2c-c22f-4c35-a7ed-c9b36646ced4");

        FeedData secondFeed = createFeed(externalEventUUId, magsObservationId, magsLoadTime, magsProvider, magsData);
        FeedEpisode episode = secondFeed.getEpisodes().get(0);

        NormalizedObservation magNormalizedObservation = normalizedObservationsMapper.getObservationsByExternalId(externalEventUUId)
                .stream()
                .filter(obs -> obs.getProvider().equals(magsProvider))
                .findFirst()
                .orElseThrow();

        assertTrue(hazardasLoadTime.isEqual(firstFeed.getUpdatedAt()));
        assertTrue(magsLoadTime.isEqual(secondFeed.getUpdatedAt()));

        assertTrue(episode.getUpdatedAt().isEqual(magsLoadTime));
        assertTrue(episode.getSourceUpdatedAt().isEqual(magNormalizedObservation.getSourceUpdatedAt()));
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