package io.kontur.eventapi.enrichment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class EnrichmentMetricsConfig {

    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;
    private final AtomicInteger enrichmentQueueSize;

    public EnrichmentMetricsConfig(MeterRegistry registry) {
        this.enrichmentSuccess = registry.counter("enrichment.success.counter");
        this.enrichmentFail = registry.counter("enrichment.fail.counter");
        this.enrichmentQueueSize = registry.gauge("enrichment.queueSize", new AtomicInteger(0));
    }

    @Bean
    public Counter enrichmentSuccessCounter() {
        return enrichmentSuccess;
    }

    @Bean
    public Counter enrichmentFailCounter() {
        return enrichmentFail;
    }

    @Bean
    public AtomicInteger enrichmentQueueSizeGauge() {
        return enrichmentQueueSize;
    }

}
