package io.kontur.eventapi.metrics.config;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.singletonList;

@Data
public class TableMetricsConfig {
    private final AtomicDouble autovacuumConditionExpectedValue;
    private final AtomicDouble autovacuumConditionActualValue;
    private final AtomicLong vacuumCount;
    private final AtomicLong autovacuumCount;
    private final AtomicLong analyseCount;
    private final AtomicLong autoAnalyseCount;

    public TableMetricsConfig(MeterRegistry registry, String tableName) {
        List<Tag> tags = singletonList(Tag.of("table", tableName));

        autovacuumConditionExpectedValue = registry.gauge("autoVacuum.condition.expected", tags, new AtomicDouble(0));
        autovacuumConditionActualValue = registry.gauge("autoVacuum.condition.actual", tags, new AtomicDouble(0));
        vacuumCount = registry.gauge("vacuum.count", tags, new AtomicLong(0));
        autovacuumCount = registry.gauge("autoVacuum.count", tags, new AtomicLong(0));
        analyseCount = registry.gauge("analyze.count", tags, new AtomicLong(0));
        autoAnalyseCount = registry.gauge("autoAnalyze.count", tags, new AtomicLong(0));
    }
}
