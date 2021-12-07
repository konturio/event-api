package io.kontur.eventapi.metrics.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.job.AbstractJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MetricsJob extends AbstractJob {

    private final FeedDao feedDao;
    private final KonturEventsDao konturEventsDao;
    private final NormalizedObservationsDao normalizedObservationsDao;
    private final DataLakeDao dataLakeDao;
    private final AtomicInteger enrichmentSkippedQueueSize;
    private final AtomicInteger enrichmentQueueSize;
    private final AtomicInteger feedCompositionQueueSize;
    private final AtomicInteger eventCombinationQueueSize;
    private final AtomicInteger normalizationQueueSize;

    protected MetricsJob(MeterRegistry meterRegistry, FeedDao feedDao, KonturEventsDao konturEventsDao,
                         NormalizedObservationsDao normalizedObservationsDao, DataLakeDao dataLakeDao,
                         AtomicInteger enrichmentQueueSizeGauge,
                         AtomicInteger enrichmentSkippedQueueSizeGauge, AtomicInteger feedCompositionQueueSizeGauge,
                         AtomicInteger eventCombinationQueueSizeGauge, AtomicInteger normalizationQueueSizeGauge) {
        super(meterRegistry);
        this.feedDao = feedDao;
        this.konturEventsDao = konturEventsDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
        this.dataLakeDao = dataLakeDao;
        this.enrichmentQueueSize = enrichmentQueueSizeGauge;
        this.enrichmentSkippedQueueSize = enrichmentSkippedQueueSizeGauge;
        this.feedCompositionQueueSize = feedCompositionQueueSizeGauge;
        this.eventCombinationQueueSize = eventCombinationQueueSizeGauge;
        this.normalizationQueueSize = normalizationQueueSizeGauge;
    }

    @Override
    public void execute() throws Exception {
        enrichmentSkippedQueueSize.set(feedDao.getEnrichmentSkippedEventsCount());
        enrichmentQueueSize.set(feedDao.getNotEnrichedEventsCount());
        feedCompositionQueueSize.set(konturEventsDao.getNotComposedEventsCount());
        eventCombinationQueueSize.set(normalizedObservationsDao.getNotRecombinedObservationsCount());
        normalizationQueueSize.set(dataLakeDao.getNotNormalizedObservationsCount());
    }

    @Override
    public String getName() {
        return "metricsJob";
    }
}
