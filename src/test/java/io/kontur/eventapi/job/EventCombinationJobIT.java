package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.com.google.common.collect.Iterables.getOnlyElement;

public class EventCombinationJobIT extends AbstractCleanableIntegrationTest {

    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final DataLakeDao dataLakeDao;
    private final FeedDao feedDao;
    private final KonturEventsDao konturEventsDao;
    private final NormalizedObservationsDao observationsDao;

    @Autowired
    public EventCombinationJobIT(NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob,
                                 DataLakeDao dataLakeDao, FeedDao feedDao, KonturEventsDao konturEventsDao,
                                 JdbcTemplate jdbcTemplate, NormalizedObservationsDao observationsDao) {
        super(jdbcTemplate, feedDao);
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.dataLakeDao = dataLakeDao;
        this.feedDao = feedDao;
        this.konturEventsDao = konturEventsDao;
        this.observationsDao = observationsDao;
    }

    @Test
    public void testSavedMags() throws IOException {
        String externalId = "996d6b0a-ce18-47d9-9bd2-b1b8fe5d967a";

        var pdcFeed = feedDao.getFeeds()
                .stream()
                .filter(feed -> feed.getAlias().equals("test-feed"))
                .findFirst()
                .orElseThrow();

        var hazardsLoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 1, 1), ZoneOffset.UTC);
        var mags01LoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 1, 2), ZoneOffset.UTC);
        var mags02LoadTime = OffsetDateTime.of(
                LocalDateTime.of(2020, 10, 1, 1, 3), ZoneOffset.UTC);

        createNormalizations(externalId, hazardsLoadTime, HP_SRV_SEARCH_PROVIDER,
                readMessageFromFile("hpsrvhazard02.json"));
        eventCombinationJob.run();
        var eventOptionalVersion1 = konturEventsDao.getEventByExternalId(externalId);
        assertTrue(eventOptionalVersion1.isPresent());
        assertEquals(1, eventOptionalVersion1.get().getObservationIds().size());

        createNormalizations(externalId, mags01LoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata02.json"));
        createNormalizations(externalId, mags02LoadTime, HP_SRV_MAG_PROVIDER, readMessageFromFile("magsdata03.json"));
        eventCombinationJob.run();

        var eventList = konturEventsDao.getEventsForRolloutEpisodes(pdcFeed.getFeedId())
                .stream()
                .filter(event -> event.equals(eventOptionalVersion1.get().getEventId()))
                .collect(toList());

        assertEquals(1, eventList.size());
        assertEquals(3, observationsDao.getObservationsByEventId(getOnlyElement(eventList)).size());
    }

    private void createNormalizations(String externalEventUUId, OffsetDateTime loadedTime, String provider,
                                      String data) {
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
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}

