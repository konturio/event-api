package io.kontur.eventapi.metrics.collector;

import io.kontur.eventapi.metrics.MetricCollector;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JobDurationMetricsCollector implements MetricCollector {

    private final MeterRegistry meterRegistry;
    private final long thresholdSeconds;
    private final String[] jobNames;
    private final Map<String, AtomicInteger> jobAlerts = new ConcurrentHashMap<>();

    public JobDurationMetricsCollector(MeterRegistry meterRegistry,
                                       @Value("${longRunningJobAlert.thresholdSeconds:600}") long thresholdSeconds,
                                       @Value("${longRunningJobAlert.jobNames:pdcMapSrvImport}") String[] jobNames) {
        this.meterRegistry = meterRegistry;
        this.thresholdSeconds = thresholdSeconds;
        this.jobNames = jobNames;

        for (String name : jobNames) {
            AtomicInteger gauge = new AtomicInteger(0);
            jobAlerts.put(name, gauge);
            Gauge.builder("job.long.running", gauge, AtomicInteger::get)
                    .tags(Tags.of("job", name))
                    .register(meterRegistry);
        }
    }

    @Override
    public void collect() {
        for (String name : jobNames) {
            LongTaskTimer timer = meterRegistry.find("job." + name + ".current").longTaskTimer();
            AtomicInteger gauge = jobAlerts.get(name);
            if (timer != null && timer.activeTasks() > 0 && timer.max(TimeUnit.SECONDS) > thresholdSeconds) {
                gauge.set(1);
            } else {
                gauge.set(0);
            }
        }
    }
}

