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
     */
    @Bean(name = "enrichmentExecutor")
    public Executor getAsyncExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("EnrichmentThread-");
        executor.initialize();
        return executor;
    }
}