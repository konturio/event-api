package io.kontur.eventapi.metrics.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;

    private final AtomicInteger enrichmentQueueSize;
    private final AtomicInteger enrichmentSkippedQueueSize;
    private final AtomicInteger feedCompositionQueueSize;
    private final AtomicInteger eventCombinationQueueSize;
    private final AtomicInteger normalizationQueueSize;

    public MetricsConfig(MeterRegistry registry) {
        this.enrichmentSuccess = registry.counter("enrichment.success.counter");
        this.enrichmentFail = registry.counter("enrichment.fail.counter");
        this.enrichmentQueueSize = registry.gauge("enrichment.queueSize", new AtomicInteger(0));
        this.enrichmentSkippedQueueSize = registry.gauge("enrichmentSkipped.queueSize", new AtomicInteger(0));
        this.feedCompositionQueueSize = registry.gauge("feedComposition.queueSize", new AtomicInteger(0));
        this.eventCombinationQueueSize = registry.gauge("eventCombination.queueSize", new AtomicInteger(0));
        this.normalizationQueueSize = registry.gauge("normalization.queueSize", new AtomicInteger(0));
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

    @Bean
    public AtomicInteger enrichmentSkippedQueueSizeGauge() {
        return enrichmentSkippedQueueSize;
    }

    @Bean
    public AtomicInteger feedCompositionQueueSizeGauge() {
        return feedCompositionQueueSize;
    }

    @Bean
    public AtomicInteger eventCombinationQueueSizeGauge() {
        return eventCombinationQueueSize;
    }

    @Bean
    public AtomicInteger normalizationQueueSizeGauge() {
        return normalizationQueueSize;
    }
}
