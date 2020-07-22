package io.kontur.eventapi.config;

import io.kontur.eventapi.pdc.job.PdcHazardImportJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class WorkerScheduler {

    private final ThreadPoolTaskExecutor taskExecutor;

    private final PdcHazardImportJob pdcHazardImportJob;

    public WorkerScheduler(ThreadPoolTaskExecutor taskExecutor,
                           PdcHazardImportJob pdcHazardImportJob) {
        this.taskExecutor = taskExecutor;
        this.pdcHazardImportJob = pdcHazardImportJob;
    }

    @Scheduled(initialDelayString = "${app.scheduler.hpSrvImport.initialDelay}", fixedDelayString = "${app.scheduler.hpSrvImport.fixedDelay}")
    public void startPdcHazardImport() {
        taskExecutor.execute(pdcHazardImportJob);
    }

}
