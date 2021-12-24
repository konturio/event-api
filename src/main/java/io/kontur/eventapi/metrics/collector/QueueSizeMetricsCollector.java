package io.kontur.eventapi.metrics.collector;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.metrics.MetricCollector;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class QueueSizeMetricsCollector implements MetricCollector {

    private final AtomicInteger enrichmentSkippedQueueSize;
    private final AtomicInteger enrichmentQueueSize;
    private final AtomicInteger feedCompositionQueueSize;
    private final AtomicInteger eventCombinationQueueSize;
    private final AtomicInteger normalizationQueueSize;

    private final FeedDao feedDao;
    private final KonturEventsDao konturEventsDao;
    private final NormalizedObservationsDao normalizedObservationsDao;
    private final DataLakeDao dataLakeDao;

    public QueueSizeMetricsCollector(AtomicInteger enrichmentSkippedQueueSizeGauge, AtomicInteger enrichmentQueueSizeGauge,
                                     AtomicInteger feedCompositionQueueSizeGauge, AtomicInteger eventCombinationQueueSizeGauge,
                                     AtomicInteger normalizationQueueSizeGauge, FeedDao feedDao, KonturEventsDao konturEventsDao,
                                     NormalizedObservationsDao normalizedObservationsDao, DataLakeDao dataLakeDao) {
        this.enrichmentSkippedQueueSize = enrichmentSkippedQueueSizeGauge;
        this.enrichmentQueueSize = enrichmentQueueSizeGauge;
        this.feedCompositionQueueSize = feedCompositionQueueSizeGauge;
        this.eventCombinationQueueSize = eventCombinationQueueSizeGauge;
        this.normalizationQueueSize = normalizationQueueSizeGauge;
        this.feedDao = feedDao;
        this.konturEventsDao = konturEventsDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public void collect() {
        enrichmentSkippedQueueSize.set(feedDao.getEnrichmentSkippedEventsCount());
        enrichmentQueueSize.set(feedDao.getNotEnrichedEventsCount());
        feedCompositionQueueSize.set(konturEventsDao.getNotComposedEventsCount());
        eventCombinationQueueSize.set(normalizedObservationsDao.getNotRecombinedObservationsCount());
        normalizationQueueSize.set(dataLakeDao.getNotNormalizedObservationsCount());
    }
}
