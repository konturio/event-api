package io.kontur.eventapi.metrics.collector;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueueSizeMetricsCollectorTest {

    @Test
    void collect_setsGaugeValuesForTotalAndEnabled() {
        AtomicInteger enrichmentSkipped = new AtomicInteger();
        AtomicInteger enrichment = new AtomicInteger();
        AtomicInteger feedComposition = new AtomicInteger();
        AtomicInteger eventCombination = new AtomicInteger();
        AtomicInteger normalization = new AtomicInteger();

        AtomicInteger enrichmentSkippedTotal = new AtomicInteger();
        AtomicInteger enrichmentTotal = new AtomicInteger();
        AtomicInteger feedCompositionTotal = new AtomicInteger();
        AtomicInteger eventCombinationTotal = new AtomicInteger();
        AtomicInteger normalizationTotal = new AtomicInteger();

        FeedDao feedDao = mock(FeedDao.class);
        KonturEventsDao konturEventsDao = mock(KonturEventsDao.class);
        NormalizedObservationsDao normalizedObservationsDao = mock(NormalizedObservationsDao.class);
        DataLakeDao dataLakeDao = mock(DataLakeDao.class);

        QueueSizeMetricsCollector collector = new QueueSizeMetricsCollector(
            enrichmentSkipped, enrichment, feedComposition, eventCombination, normalization,
            enrichmentSkippedTotal, enrichmentTotal, feedCompositionTotal, eventCombinationTotal, normalizationTotal,
            feedDao, konturEventsDao, normalizedObservationsDao, dataLakeDao,
            new String[]{"feed1"}, new String[]{"provider1"}, new String[]{"provider2"}
        );

        when(feedDao.getEnrichmentSkippedEventsCountForFeedsWithEnrichment()).thenReturn(1);
        when(feedDao.getNotEnrichedEventsCountForFeedsWithEnrichment()).thenReturn(2);
        when(konturEventsDao.getNotComposedEventsCountForFeeds(anyList())).thenReturn(3);
        when(normalizedObservationsDao.getNotRecombinedObservationsCountForProviders(anyList())).thenReturn(4);
        when(dataLakeDao.getNotNormalizedObservationsCountForProviders(anyList())).thenReturn(5);

        when(feedDao.getEnrichmentSkippedEventsCount()).thenReturn(6);
        when(feedDao.getNotEnrichedEventsCount()).thenReturn(7);
        when(konturEventsDao.getNotComposedEventsCount()).thenReturn(8);
        when(normalizedObservationsDao.getNotRecombinedObservationsCount()).thenReturn(9);
        when(dataLakeDao.getNotNormalizedObservationsCount()).thenReturn(10);

        collector.collect();

        assertEquals(1, enrichmentSkipped.get(), "Enrichment skipped enabled queue size mismatch");
        assertEquals(2, enrichment.get(), "Enrichment enabled queue size mismatch");
        assertEquals(3, feedComposition.get(), "Feed composition enabled queue size mismatch");
        assertEquals(4, eventCombination.get(), "Event combination enabled queue size mismatch");
        assertEquals(5, normalization.get(), "Normalization enabled queue size mismatch");

        assertEquals(6, enrichmentSkippedTotal.get(), "Enrichment skipped total queue size mismatch");
        assertEquals(7, enrichmentTotal.get(), "Enrichment total queue size mismatch");
        assertEquals(8, feedCompositionTotal.get(), "Feed composition total queue size mismatch");
        assertEquals(9, eventCombinationTotal.get(), "Event combination total queue size mismatch");
        assertEquals(10, normalizationTotal.get(), "Normalization total queue size mismatch");
    }
}
