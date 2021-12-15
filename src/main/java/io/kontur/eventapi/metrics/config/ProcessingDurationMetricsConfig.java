package io.kontur.eventapi.metrics.config;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;

@Data
public class ProcessingDurationMetricsConfig {
    private final AtomicDouble max;
    private final AtomicDouble min;
    private final AtomicDouble avg;
    private final AtomicInteger count;

    public ProcessingDurationMetricsConfig(MeterRegistry registry, String stage) {
        List<Tag> tags = singletonList(Tag.of("stage", stage));

        max = registry.gauge("max.processing.duration.seconds", tags, new AtomicDouble(0));
        min = registry.gauge("min.processing.duration.seconds", tags, new AtomicDouble(0));
        avg = registry.gauge("avg.processing.duration.seconds", tags, new AtomicDouble(0));
        count = registry.gauge("count.processing.observations", tags, new AtomicInteger(0));
    }

    public void resetAll() {
        this.max.set(0);
        this.min.set(0);
        this.avg.set(0);
        this.count.set(0);

    }
}
