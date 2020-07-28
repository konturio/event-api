package io.kontur.eventapi.config;

import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class WorkerScheduler {

    private final Logger LOG = LoggerFactory.getLogger(WorkerScheduler.class);
    private final ThreadPoolTaskExecutor taskExecutor;
    private final HpSrvSearchJob hpSrvSearchJob;

    @Value("scheduler.hpSrvImport.enable")
    private String hpSrvImportEnabled;

    public WorkerScheduler(ThreadPoolTaskExecutor taskExecutor,
                           HpSrvSearchJob hpSrvSearchJob) {
        this.taskExecutor = taskExecutor;
        this.hpSrvSearchJob = hpSrvSearchJob;
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startPdcHazardImport() {
        if (Boolean.parseBoolean(hpSrvImportEnabled)) {
            taskExecutor.execute(hpSrvSearchJob);
        } else {
            LOG.info("HpSrv import job invocation is skipped");
        }
    }
}
