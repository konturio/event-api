package io.kontur.eventapi.config;

import io.kontur.eventapi.normalization.NormalizationJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.recombination.RecombinationJob;
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
    private final NormalizationJob normalizationJob;
    private final RecombinationJob recombinationJob;

    @Value("${scheduler.hpSrvImport.enable}")
    private String hpSrvImportEnabled;
    @Value("${scheduler.normalization.enable}")
    private String normalizationEnabled;
    @Value("${scheduler.recombination.enable}")
    private String recombinationEnabled;

    public WorkerScheduler(ThreadPoolTaskExecutor taskExecutor,
                           HpSrvSearchJob hpSrvSearchJob,
                           NormalizationJob normalizationJob,
                           RecombinationJob recombinationJob) {
        this.taskExecutor = taskExecutor;
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.normalizationJob = normalizationJob;
        this.recombinationJob = recombinationJob;
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startPdcHazardImport() {
        if (Boolean.parseBoolean(hpSrvImportEnabled)) {
            taskExecutor.execute(hpSrvSearchJob);
        } else {
            LOG.info("HpSrv import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.normalization.initialDelay}", fixedDelayString = "${scheduler.normalization.fixedDelay}")
    public void startNormalization() {
        if (Boolean.parseBoolean(normalizationEnabled)) {
            taskExecutor.execute(normalizationJob);
        } else {
            LOG.info("Normalization job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.recombination.initialDelay}", fixedDelayString = "${scheduler.recombination.fixedDelay}")
    public void startRecombinationJob() {
        if (Boolean.parseBoolean(recombinationEnabled)) {
            taskExecutor.execute(recombinationJob);
        } else {
            LOG.info("Recombination job invocation is skipped");
        }
    }
}
