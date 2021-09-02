package io.kontur.eventapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class EnrichmentExecutorConfig {
    /**
     * This bean configures executor for {@link io.kontur.eventapi.enrichment.EventEnrichmentTask}.
     * It sets number of threads to be 3, so we don't harm Insights API service,
     * and queue capacity to 500 because we fetch only 100 {@link io.kontur.eventapi.entity.FeedData}
     * records to process for each feed, so 500 is more than enough.
     */
    @Bean(name = "enrichmentExecutor")
    public Executor getAsyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("EnrichmentThread-");
        executor.initialize();
        return executor;
    }
}