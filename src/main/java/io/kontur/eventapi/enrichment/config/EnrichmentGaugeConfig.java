package io.kontur.eventapi.enrichment.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class EnrichmentGaugeConfig {

    private final AtomicInteger enrichmentSuccess;
    private final AtomicInteger enrichmentFail;
    private final AtomicInteger enrichmentQueueSize;

    public EnrichmentGaugeConfig(MeterRegistry registry) {
        this.enrichmentSuccess = registry.gauge("enrichment.success", new AtomicInteger(0));
        this.enrichmentFail = registry.gauge("enrichment.fail", new AtomicInteger(0));
        this.enrichmentQueueSize = registry.gauge("enrichment.queueSize", new AtomicInteger(0));
    }

    @Bean
    public AtomicInteger enrichmentSuccessGauge() {
        return enrichmentSuccess;
    }

    @Bean
    public AtomicInteger enrichmentFailGauge() {
        return enrichmentFail;
    }

    @Bean
    public AtomicInteger enrichmentQueueSizeGauge() {
        return enrichmentQueueSize;
    }
}
