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

    private final AtomicInteger enrichmentSkippedQueueSizeTotal;
    private final AtomicInteger enrichmentQueueSizeTotal;
    private final AtomicInteger feedCompositionQueueSizeTotal;
    private final AtomicInteger eventCombinationQueueSizeTotal;
    private final AtomicInteger normalizationQueueSizeTotal;

    private final FeedDao feedDao;
    private final KonturEventsDao konturEventsDao;
    private final NormalizedObservationsDao normalizedObservationsDao;
    private final DataLakeDao dataLakeDao;

    private final String[] feedAliases;
    private final String[] eventCombinationProviders;
    private final String[] normalizationProviders;

    public QueueSizeMetricsCollector(AtomicInteger enrichmentSkippedQueueSizeGauge, AtomicInteger enrichmentQueueSizeGauge,
                                     AtomicInteger feedCompositionQueueSizeGauge, AtomicInteger eventCombinationQueueSizeGauge,
                                     AtomicInteger normalizationQueueSizeGauge,
                                     AtomicInteger enrichmentSkippedQueueSizeTotalGauge, AtomicInteger enrichmentQueueSizeTotalGauge,
                                     AtomicInteger feedCompositionQueueSizeTotalGauge, AtomicInteger eventCombinationQueueSizeTotalGauge,
                                     AtomicInteger normalizationQueueSizeTotalGauge, FeedDao feedDao, KonturEventsDao konturEventsDao,
                                     NormalizedObservationsDao normalizedObservationsDao, DataLakeDao dataLakeDao,
                                     @org.springframework.beans.factory.annotation.Value("${scheduler.feedComposition.alias}") String[] feedAliases,
                                     @org.springframework.beans.factory.annotation.Value("${scheduler.eventCombination.providers}") String[] eventCombinationProviders,
                                     @org.springframework.beans.factory.annotation.Value("${scheduler.normalization.providers}") String[] normalizationProviders) {
        this.enrichmentSkippedQueueSize = enrichmentSkippedQueueSizeGauge;
        this.enrichmentQueueSize = enrichmentQueueSizeGauge;
        this.feedCompositionQueueSize = feedCompositionQueueSizeGauge;
        this.eventCombinationQueueSize = eventCombinationQueueSizeGauge;
        this.normalizationQueueSize = normalizationQueueSizeGauge;
        this.enrichmentSkippedQueueSizeTotal = enrichmentSkippedQueueSizeTotalGauge;
        this.enrichmentQueueSizeTotal = enrichmentQueueSizeTotalGauge;
        this.feedCompositionQueueSizeTotal = feedCompositionQueueSizeTotalGauge;
        this.eventCombinationQueueSizeTotal = eventCombinationQueueSizeTotalGauge;
        this.normalizationQueueSizeTotal = normalizationQueueSizeTotalGauge;
        this.feedDao = feedDao;
        this.konturEventsDao = konturEventsDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
        this.dataLakeDao = dataLakeDao;
        this.feedAliases = feedAliases;
        this.eventCombinationProviders = eventCombinationProviders;
        this.normalizationProviders = normalizationProviders;
    }

    @Override
    public void collect() {
        enrichmentSkippedQueueSize.set(feedDao.getEnrichmentSkippedEventsCountForFeedsWithEnrichment());
        enrichmentQueueSize.set(feedDao.getNotEnrichedEventsCountForFeedsWithEnrichment());
        feedCompositionQueueSize.set(konturEventsDao.getNotComposedEventsCountForFeeds(java.util.Arrays.asList(feedAliases)));
        eventCombinationQueueSize.set(normalizedObservationsDao.getNotRecombinedObservationsCountForProviders(java.util.Arrays.asList(eventCombinationProviders)));
        normalizationQueueSize.set(dataLakeDao.getNotNormalizedObservationsCountForProviders(java.util.Arrays.asList(normalizationProviders)));

        enrichmentSkippedQueueSizeTotal.set(feedDao.getEnrichmentSkippedEventsCount());
        enrichmentQueueSizeTotal.set(feedDao.getNotEnrichedEventsCount());
        feedCompositionQueueSizeTotal.set(konturEventsDao.getNotComposedEventsCount());
        eventCombinationQueueSizeTotal.set(normalizedObservationsDao.getNotRecombinedObservationsCount());
        normalizationQueueSizeTotal.set(dataLakeDao.getNotNormalizedObservationsCount());
    }
}
