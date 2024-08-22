package io.kontur.eventapi.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class FirmsFeedCompositionExecutorConfig {
    /**
     * This bean configures executor for {@link io.kontur.eventapi.firms.episodecomposition.FirmsFeedCompositionJob}.
     */
    @Bean(name = "firmsFeedCompositionExecutor")
    public ExecutorService firmsFeedCompositionExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("FirmsFeedCompositionThread-%d")
                .build();
        return Executors.newFixedThreadPool(10, threadFactory);
    }
}
