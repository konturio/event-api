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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedCompositionJobTest extends AbstractIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final DataLakeMapper dataLakeMapper;
    private final FeedMapper feedMapper;

    @Autowired
    public FeedCompositionJobTest(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, DataLakeMapper dataLakeMapper, FeedMapper feedMapper) {
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.dataLakeMapper = dataLakeMapper;
        this.feedMapper = feedMapper;
    }

    @Test
    public void testUpdateDates() throws IOException {
        String hpSrvHazardExternalUUID = "54a5c6a9-f495-444c-9246-27e231383cfe";
        var hpSrvHazardDataLoad = OffsetDateTime.now(ZoneOffset.UTC);
        var hpSrvHazardObservationUUID = UUID.fromString("e27f16d9-14b7-46c1-a611-fc7824706220");
        var hpSrvHazard = new DataLake(
                hpSrvHazardObservationUUID,
                hpSrvHazardExternalUUID,
                OffsetDateTime.now(ZoneOffset.UTC),
                hpSrvHazardDataLoad,
                "hpSrvSearch",
                readMessageFromFile("hpsrvhazard.json")

        );

        String hpSrvMagExternalUUID = "c6d90405-1ffe-43db-9f37-d5667abf01d2";
        var hpSrvMagDataLoad = OffsetDateTime.now(ZoneOffset.UTC);
        var hpSrvMagObservationUUID = UUID.fromString("f6616c33-74c7-4a02-a689-f2c6902a1b24");
        var hpSrvMag = new DataLake(
                hpSrvMagObservationUUID,
                hpSrvMagExternalUUID,
                hpSrvMagDataLoad,
                "hpSrvMag",
                readMessageFromFile("magsdata.json")
        );

        String pdcSqsExternalUUID = "fb117f8b-845c-5484-9e86-2864d4c6b88b";
        var pdcSqsDataLoad = OffsetDateTime.now(ZoneOffset.UTC);
        var pdcSqsObservationUUID = UUID.fromString("856fdeff-ae2b-4805-a945-f594f2d03c6a");
        var pdcSqs = new DataLake(
                pdcSqsObservationUUID,
                pdcSqsExternalUUID,
                pdcSqsDataLoad,
                "pdcSqs",
                readMessageFromFile("pdcsqsobservdata.json")
        );

        List<DataLake> observations = List.of(hpSrvHazard, hpSrvMag, pdcSqs);

        observations.forEach(dataLakeMapper::create);
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        FeedData hpsrvFeed = feedMapper.getFeedDataByUpdatedAt(hpSrvHazardDataLoad).orElseThrow();
        assertEquals(hpSrvHazardObservationUUID, hpsrvFeed.getObservations().get(0));

        FeedData hpsrvMagFeed = feedMapper.getFeedDataByUpdatedAt(hpSrvMagDataLoad).orElseThrow();
        assertEquals(hpSrvMagObservationUUID, hpsrvMagFeed.getObservations().get(0));

        FeedData pdcSqsFeed = feedMapper.getFeedDataByUpdatedAt(pdcSqsDataLoad).orElseThrow();
        assertEquals(pdcSqsObservationUUID, pdcSqsFeed.getObservations().get(0));
    }

    private String readMessageFromFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }


}