package io.kontur.eventapi.config;

import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class WorkerScheduler {

    private final ThreadPoolTaskExecutor taskExecutor;

    private final HpSrvSearchJob hpSrvSearchJob;

    public WorkerScheduler(ThreadPoolTaskExecutor taskExecutor,
                           HpSrvSearchJob hpSrvSearchJob) {
        this.taskExecutor = taskExecutor;
        this.hpSrvSearchJob = hpSrvSearchJob;
    }

    @Scheduled(initialDelayString = "${app.scheduler.hpSrvImport.initialDelay}", fixedDelayString = "${app.scheduler.hpSrvImport.fixedDelay}")
    public void startPdcHazardImport() {
        taskExecutor.execute(hpSrvSearchJob);
    }

}
