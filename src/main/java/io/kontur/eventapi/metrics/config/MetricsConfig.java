package io.kontur.eventapi.metrics.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricsConfig {

    private static final String DATA_LAKE = "data_lake";
    private static final String NORMALIZED_OBSERVATIONS = "normalized_observations";
    private static final String KONTUR_EVENTS = "kontur_events";
    private static final String FEED_EVENT_STATUS = "feed_event_status";
    private static final String FEED_DATA = "feed_data";
    private static final String FEEDS = "feeds";

    private final Map<String, TableMetricsConfig> tableMetrics;

    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;

    private final AtomicInteger enrichmentQueueSize;
    private final AtomicInteger enrichmentSkippedQueueSize;
    private final AtomicInteger feedCompositionQueueSize;
    private final AtomicInteger eventCombinationQueueSize;
    private final AtomicInteger normalizationQueueSize;

    public MetricsConfig(MeterRegistry registry) {
        tableMetrics = Map.of(
                DATA_LAKE, new TableMetricsConfig(registry, DATA_LAKE),
                NORMALIZED_OBSERVATIONS, new TableMetricsConfig(registry, NORMALIZED_OBSERVATIONS),
                KONTUR_EVENTS, new TableMetricsConfig(registry, KONTUR_EVENTS),
                FEED_EVENT_STATUS, new TableMetricsConfig(registry, FEED_EVENT_STATUS),
                FEED_DATA, new TableMetricsConfig(registry, FEED_DATA),
                FEEDS, new TableMetricsConfig(registry, FEEDS)
        );

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

    @Bean Map<String, TableMetricsConfig> tableMetrics() {
        return tableMetrics;
    }
}
