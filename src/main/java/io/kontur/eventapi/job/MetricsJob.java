package io.kontur.eventapi.job;

import io.kontur.eventapi.metrics.MetricCollector;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MetricsJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsJob.class);
    private final List<MetricCollector> collectors;

    protected MetricsJob(MeterRegistry meterRegistry, List<MetricCollector> collectors) {
        super(meterRegistry);
        this.collectors = collectors;
    }

    @Override
    public void execute() throws Exception {
        collectors.forEach(collector -> {
            try {
                collector.collect();
            } catch (Exception e) {
                LOG.error("Failed to collect metrics", e);
            }
        });
    }

    @Override
    public String getName() {
        return "metricsJob";
    }
}
