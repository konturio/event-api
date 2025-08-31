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

    public static final String NORMALIZATION = "normalization";
    public static final String RECOMBINATION = "recombination";
    public static final String COMPOSITION = "composition";
    public static final String ENRICHMENT = "enrichment";

    private final Map<String, TableMetricsConfig> tableMetrics;
    private final Map<String, ProcessingDurationMetricsConfig> processingDurationMetrics;

    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;

    private final AtomicInteger enrichmentQueueSize;
    private final AtomicInteger enrichmentSkippedQueueSize;
    private final AtomicInteger feedCompositionQueueSize;
    private final AtomicInteger eventCombinationQueueSize;
    private final AtomicInteger normalizationQueueSize;

    private final AtomicInteger enrichmentQueueSizeTotal;
    private final AtomicInteger enrichmentSkippedQueueSizeTotal;
    private final AtomicInteger feedCompositionQueueSizeTotal;
    private final AtomicInteger eventCombinationQueueSizeTotal;
    private final AtomicInteger normalizationQueueSizeTotal;

    private final AtomicInteger sqsQueueSize;
    private final AtomicInteger sqsDLQueueSize;

    public MetricsConfig(MeterRegistry registry) {
        tableMetrics = Map.of(
                DATA_LAKE, new TableMetricsConfig(registry, DATA_LAKE),
                NORMALIZED_OBSERVATIONS, new TableMetricsConfig(registry, NORMALIZED_OBSERVATIONS),
                KONTUR_EVENTS, new TableMetricsConfig(registry, KONTUR_EVENTS),
                FEED_EVENT_STATUS, new TableMetricsConfig(registry, FEED_EVENT_STATUS),
                FEED_DATA, new TableMetricsConfig(registry, FEED_DATA),
                FEEDS, new TableMetricsConfig(registry, FEEDS)
        );

        processingDurationMetrics = Map.of(
                NORMALIZATION, new ProcessingDurationMetricsConfig(registry, NORMALIZATION),
                RECOMBINATION, new ProcessingDurationMetricsConfig(registry, RECOMBINATION),
                COMPOSITION, new ProcessingDurationMetricsConfig(registry, COMPOSITION),
                ENRICHMENT, new ProcessingDurationMetricsConfig(registry, ENRICHMENT)
        );

        this.enrichmentSuccess = registry.counter("enrichment.success.counter");
        this.enrichmentFail = registry.counter("enrichment.fail.counter");
        this.enrichmentQueueSize = registry.gauge("enrichment.queueSize", new AtomicInteger(0));
        this.enrichmentSkippedQueueSize = registry.gauge("enrichmentSkipped.queueSize", new AtomicInteger(0));
        this.feedCompositionQueueSize = registry.gauge("feedComposition.queueSize", new AtomicInteger(0));
        this.eventCombinationQueueSize = registry.gauge("eventCombination.queueSize", new AtomicInteger(0));
        this.normalizationQueueSize = registry.gauge("normalization.queueSize", new AtomicInteger(0));

        this.enrichmentQueueSizeTotal = registry.gauge("enrichment.totalQueueSize", new AtomicInteger(0));
        this.enrichmentSkippedQueueSizeTotal = registry.gauge("enrichmentSkipped.totalQueueSize", new AtomicInteger(0));
        this.feedCompositionQueueSizeTotal = registry.gauge("feedComposition.totalQueueSize", new AtomicInteger(0));
        this.eventCombinationQueueSizeTotal = registry.gauge("eventCombination.totalQueueSize", new AtomicInteger(0));
        this.normalizationQueueSizeTotal = registry.gauge("normalization.totalQueueSize", new AtomicInteger(0));

        this.sqsQueueSize = registry.gauge("sqs.queueSize", new AtomicInteger(0));
        this.sqsDLQueueSize = registry.gauge("sqs.dl.queueSize", new AtomicInteger(0));
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
    public AtomicInteger enrichmentQueueSizeTotalGauge() {
        return enrichmentQueueSizeTotal;
    }

    @Bean
    public AtomicInteger enrichmentSkippedQueueSizeGauge() {
        return enrichmentSkippedQueueSize;
    }

    @Bean
    public AtomicInteger enrichmentSkippedQueueSizeTotalGauge() {
        return enrichmentSkippedQueueSizeTotal;
    }

    @Bean
    public AtomicInteger feedCompositionQueueSizeGauge() {
        return feedCompositionQueueSize;
    }

    @Bean
    public AtomicInteger feedCompositionQueueSizeTotalGauge() {
        return feedCompositionQueueSizeTotal;
    }

    @Bean
    public AtomicInteger eventCombinationQueueSizeGauge() {
        return eventCombinationQueueSize;
    }

    @Bean
    public AtomicInteger eventCombinationQueueSizeTotalGauge() {
        return eventCombinationQueueSizeTotal;
    }

    @Bean
    public AtomicInteger normalizationQueueSizeGauge() {
        return normalizationQueueSize;
    }

    @Bean
    public AtomicInteger normalizationQueueSizeTotalGauge() {
        return normalizationQueueSizeTotal;
    }

    @Bean Map<String, TableMetricsConfig> tableMetrics() {
        return tableMetrics;
    }

    @Bean
    Map<String, ProcessingDurationMetricsConfig> processingDurationMetrics() {
        return processingDurationMetrics;
    }

    @Bean
    AtomicInteger sqsQueueSize() {
        return sqsQueueSize;
    }

    @Bean
    AtomicInteger sqsDLQueueSize() {
        return sqsDLQueueSize;
    }
}
