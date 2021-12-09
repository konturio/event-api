package io.kontur.eventapi.metrics.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dao.GeneralDao;
import io.kontur.eventapi.entity.PgSetting;
import io.kontur.eventapi.entity.PgStatTable;
import io.kontur.eventapi.metrics.config.TableMetricsConfig;
import io.kontur.eventapi.job.AbstractJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.Double.parseDouble;

@Component
public class MetricsJob extends AbstractJob {

    private final FeedDao feedDao;
    private final KonturEventsDao konturEventsDao;
    private final NormalizedObservationsDao normalizedObservationsDao;
    private final DataLakeDao dataLakeDao;
    private final GeneralDao generalDao;
    private final AtomicInteger enrichmentSkippedQueueSize;
    private final AtomicInteger enrichmentQueueSize;
    private final AtomicInteger feedCompositionQueueSize;
    private final AtomicInteger eventCombinationQueueSize;
    private final AtomicInteger normalizationQueueSize;
    private final Map<String, TableMetricsConfig> tableMetrics;

    protected MetricsJob(MeterRegistry meterRegistry, FeedDao feedDao, KonturEventsDao konturEventsDao,
                         NormalizedObservationsDao normalizedObservationsDao, DataLakeDao dataLakeDao,
                         GeneralDao generalDao, AtomicInteger enrichmentQueueSizeGauge,
                         AtomicInteger enrichmentSkippedQueueSizeGauge, AtomicInteger feedCompositionQueueSizeGauge,
                         AtomicInteger eventCombinationQueueSizeGauge, AtomicInteger normalizationQueueSizeGauge,
                         Map<String, TableMetricsConfig> tableMetrics) {
        super(meterRegistry);
        this.feedDao = feedDao;
        this.konturEventsDao = konturEventsDao;
        this.normalizedObservationsDao = normalizedObservationsDao;
        this.dataLakeDao = dataLakeDao;
        this.generalDao = generalDao;
        this.enrichmentQueueSize = enrichmentQueueSizeGauge;
        this.enrichmentSkippedQueueSize = enrichmentSkippedQueueSizeGauge;
        this.feedCompositionQueueSize = feedCompositionQueueSizeGauge;
        this.eventCombinationQueueSize = eventCombinationQueueSizeGauge;
        this.normalizationQueueSize = normalizationQueueSizeGauge;
        this.tableMetrics = tableMetrics;
    }

    @Override
    public void execute() throws Exception {
        enrichmentSkippedQueueSize.set(feedDao.getEnrichmentSkippedEventsCount());
        enrichmentQueueSize.set(feedDao.getNotEnrichedEventsCount());
        feedCompositionQueueSize.set(konturEventsDao.getNotComposedEventsCount());
        eventCombinationQueueSize.set(normalizedObservationsDao.getNotRecombinedObservationsCount());
        normalizationQueueSize.set(dataLakeDao.getNotNormalizedObservationsCount());

        Map<String, PgStatTable> pgStatTables = generalDao.getPgStatTables();
        Map<String, PgSetting> pgSettings = generalDao.getPgSettings();

        tableMetrics.forEach((key, value) -> {
            PgStatTable pgStatTable = pgStatTables.get(key);
            value.getVacuumCount().set(checkNotNull(pgStatTable.getVacuumCount()));
            value.getAutovacuumCount().set(checkNotNull(pgStatTable.getAutoVacuumCount()));
            value.getAnalyseCount().set(checkNotNull(pgStatTable.getAnalyzeCount()));
            value.getAutoAnalyseCount().set(checkNotNull(pgStatTable.getAutoAnalyzeCount()));
            value.getAutovacuumConditionActualValue().set(checkNotNull(pgStatTable.getDeadTupCount()));

            double autovacuumScaleFactor = parseDouble(pgSettings.get("autovacuum_vacuum_scale_factor").getSetting());
            double autovacuumThreshold = parseDouble(pgSettings.get("autovacuum_vacuum_threshold").getSetting());

            value.getAutovacuumConditionExpectedValue()
                    .set(autovacuumThreshold + autovacuumScaleFactor * checkNotNull(pgStatTable.getLiveTupCount()));
        });
    }

    private Long checkNotNull(Long value) {
        return value == null ? 0L : value;
    }

    @Override
    public String getName() {
        return "metricsJob";
    }
}
