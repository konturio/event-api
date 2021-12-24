package io.kontur.eventapi.metrics.collector;

import io.kontur.eventapi.dao.GeneralDao;
import io.kontur.eventapi.entity.ProcessingDuration;
import io.kontur.eventapi.metrics.MetricCollector;
import io.kontur.eventapi.metrics.config.ProcessingDurationMetricsConfig;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.kontur.eventapi.metrics.config.MetricsConfig.*;
import static io.kontur.eventapi.metrics.config.MetricsConfig.ENRICHMENT;
import static java.time.temporal.ChronoUnit.MINUTES;

@Component
public class ProcessingDurationMetricsCollector implements MetricCollector {

    private final Map<String, ProcessingDurationMetricsConfig> processingDurationMetrics;
    private Map<String, OffsetDateTime> latestProcessedAt;

    private final GeneralDao generalDao;

    public ProcessingDurationMetricsCollector(Map<String, ProcessingDurationMetricsConfig> processingDurationMetrics,
                                              GeneralDao generalDao) {
        this.processingDurationMetrics = processingDurationMetrics;
        this.generalDao = generalDao;
        OffsetDateTime initialLatestProcessedAt = OffsetDateTime.now().minus(5, MINUTES);
        latestProcessedAt = new HashMap<>();
        latestProcessedAt.put(NORMALIZATION, initialLatestProcessedAt);
        latestProcessedAt.put(RECOMBINATION, initialLatestProcessedAt);
        latestProcessedAt.put(COMPOSITION, initialLatestProcessedAt);
        latestProcessedAt.put(ENRICHMENT, initialLatestProcessedAt);
    }

    @Override
    public void collect() {
        processingDurationMetrics.forEach((stage, metric) -> {
            Optional<ProcessingDuration> durationOpt = generalDao.getProcessingDuration(stage, latestProcessedAt.get(stage));
            if (durationOpt.isPresent()) {
                ProcessingDuration duration = durationOpt.get();
                metric.getAvg().set(checkNotNull(duration.getAvg()));
                metric.getMax().set(checkNotNull(duration.getMax()));
                metric.getMin().set(checkNotNull(duration.getMin()));
                metric.getCount().set(checkNotNull(duration.getCount()));
                if (duration.getLatestProcessedAt() != null)
                    latestProcessedAt.put(stage, duration.getLatestProcessedAt());
            } else {
                metric.resetAll();
            }
        });
    }

    private Double checkNotNull(Double value) {
        return value == null ? 0.0 : value;
    }

    private Integer checkNotNull(Integer value) {
        return value == null ? 0 : value;
    }

}
