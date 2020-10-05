package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.FeedData;
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

    @Autowired
    public FeedCompositionJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, DataLakeMapper dataLakeMapper, FeedMapper feedMapper) {
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeMapper = dataLakeMapper;
        this.feedMapper = feedMapper;
    }

    @Test
    public void testUpdateDates() throws IOException {
        String externalEventUUId = "0457178b-45c1-492f-bbc1-61ca14389a31";

        var firstObservation = new DataLake();
        firstObservation.setObservationId(UUID.fromString("8ac2acda-0ef7-4976-876b-580b82c29bea"));
        firstObservation.setExternalId(externalEventUUId);
        firstObservation.setLoadedAt(OffsetDateTime.of(LocalDateTime.of(2020, 2, 2, 2, 2), ZoneOffset.UTC));
        firstObservation.setProvider("hpSrvSearch");
        firstObservation.setData(readMessageFromFile("hpsrvhazard.json"));

        dataLakeMapper.create(firstObservation);
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        FeedData firstFeed = feedMapper.searchForEvents(
                "pdc-v0",
                OffsetDateTime.of(LocalDateTime.of(2020, 1, 2, 2, 2), ZoneOffset.UTC),
                0,
                1
        ).get(0);

        var secondObservation = new DataLake();
        secondObservation.setObservationId(UUID.fromString("10248c2c-c22f-4c35-a7ed-c9b36646ced4"));
        secondObservation.setExternalId(externalEventUUId);
        secondObservation.setLoadedAt(OffsetDateTime.of(LocalDateTime.of(2019, 2, 2, 2, 2), ZoneOffset.UTC));
        secondObservation.setProvider("hpSrvMag");
        secondObservation.setData(readMessageFromFile("magsdata.json"));

        dataLakeMapper.create(secondObservation);
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        FeedData secondFeed = feedMapper.searchForEvents(
                "pdc-v0",
                OffsetDateTime.of(LocalDateTime.of(2019, 1, 2, 2, 2), ZoneOffset.UTC),
                0,
                1
        ).get(0);

        assertTrue(firstFeed.getUpdatedAt().isAfter(secondFeed.getUpdatedAt()));
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }


}