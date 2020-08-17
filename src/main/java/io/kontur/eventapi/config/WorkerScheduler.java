package io.kontur.eventapi.config;

import io.kontur.eventapi.combination.EventCombinationJob;
import io.kontur.eventapi.feed.FeedCompositionJob;
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
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;

    @Value("${scheduler.hpSrvImport.enable}")
    private String hpSrvImportEnabled;
    @Value("${scheduler.normalization.enable}")
    private String normalizationEnabled;
    @Value("${scheduler.eventCombination.enable}")
    private String eventCombinationEnabled;
    @Value("${scheduler.feedComposition.enable}")
    private String feedCompositionEnabled;

    public WorkerScheduler(ThreadPoolTaskExecutor taskExecutor,
                           HpSrvSearchJob hpSrvSearchJob,
                           NormalizationJob normalizationJob,
                           EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob) {
        this.taskExecutor = taskExecutor;
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
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

    @Scheduled(initialDelayString = "${scheduler.eventCombination.initialDelay}", fixedDelayString = "${scheduler.eventCombination.fixedDelay}")
    public void startCombinationJob() {
        if (Boolean.parseBoolean(eventCombinationEnabled)) {
            taskExecutor.execute(eventCombinationJob);
        } else {
            LOG.info("Combination job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.feedComposition.initialDelay}", fixedDelayString = "${scheduler.feedComposition.fixedDelay}")
    public void startFeedCompositionJob() {
        if (Boolean.parseBoolean(feedCompositionEnabled)) {
            taskExecutor.execute(feedCompositionJob);
        } else {
            LOG.info("Feed Compose job invocation is skipped");
        }
    }
}
