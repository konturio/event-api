package io.kontur.eventapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class EnrichmentExecutorConfig {

    @Value("${enrichmentExecutor.poolSize:5}")
    private int enrichmentExecutorPoolSize;

    /**
     * This bean configures executor for {@link io.kontur.eventapi.enrichment.EventEnrichmentTask}.
     * Thread count is configurable with {@code enrichmentExecutor.poolSize} property.
     */
    @Bean(name = "enrichmentExecutor")
    public Executor getAsyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(enrichmentExecutorPoolSize);
        executor.setMaxPoolSize(enrichmentExecutorPoolSize);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("EnrichmentThread-");
        executor.initialize();
        return executor;
    }
}