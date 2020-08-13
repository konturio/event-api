package io.kontur.eventapi.config;

import io.kontur.eventapi.combination.CombinationJob;
import io.kontur.eventapi.normalization.NormalizationJob;
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
    private final NormalizationJob normalizationJob;
    private final CombinationJob combinationJob;

    @Value("${scheduler.hpSrvImport.enable}")
    private String hpSrvImportEnabled;
    @Value("${scheduler.normalization.enable}")
    private String normalizationEnabled;
    @Value("${scheduler.combination.enable}")
    private String combinationEnabled;

    public WorkerScheduler(ThreadPoolTaskExecutor taskExecutor,
                           HpSrvSearchJob hpSrvSearchJob,
                           NormalizationJob normalizationJob,
                           CombinationJob combinationJob) {
        this.taskExecutor = taskExecutor;
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.normalizationJob = normalizationJob;
        this.combinationJob = combinationJob;
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

    @Scheduled(initialDelayString = "${scheduler.combination.initialDelay}", fixedDelayString = "${scheduler.combination.fixedDelay}")
    public void startCombinationJob() {
        if (Boolean.parseBoolean(combinationEnabled)) {
            taskExecutor.execute(combinationJob);
        } else {
            LOG.info("Combination job invocation is skipped");
        }
    }
}
