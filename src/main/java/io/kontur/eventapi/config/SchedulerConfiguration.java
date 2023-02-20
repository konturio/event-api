package io.kontur.eventapi.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.PdcMapSrvSearchJobs;
import io.kontur.eventapi.pdc.client.PdcMapSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableScheduling
public class SchedulerConfiguration implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    /**
     * Max threads count for scheduled thread pool can not be configured.
     * We set thread name, so we can detect how many job threads are
     * active at any time.
     */
    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ScheduledJobThread-%d")
                .build();
        return Executors.newScheduledThreadPool(10, threadFactory);
    }

    @Bean
    public PdcMapSrvSearchJobs getPdcMapSrvSearchJobs(MeterRegistry meterRegistry, PdcMapSrvClient pdcMapSrvClient,
                                                      PdcDataLakeConverter pdcDataLakeConverter, DataLakeDao dataLakeDao) {
        return new PdcMapSrvSearchJobs(meterRegistry, pdcMapSrvClient, pdcDataLakeConverter, dataLakeDao);
    }

}
