package io.kontur.eventapi.config;

import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.pdc.job.HpSrvMagsJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkerScheduler {

    private final Logger LOG = LoggerFactory.getLogger(WorkerScheduler.class);
    private final HpSrvSearchJob hpSrvSearchJob;
    private final HpSrvMagsJob hpSrvMagsJob;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;

    @Value("${scheduler.hpSrvImport.enable}")
    private String hpSrvImportEnabled;
    @Value("${scheduler.hpSrvMagsImport.enable}")
    private String hpSrvMagsImportEnabled;
    @Value("${scheduler.normalization.enable}")
    private String normalizationEnabled;
    @Value("${scheduler.eventCombination.enable}")
    private String eventCombinationEnabled;
    @Value("${scheduler.feedComposition.enable}")
    private String feedCompositionEnabled;

    public WorkerScheduler(HpSrvSearchJob hpSrvSearchJob,
                           HpSrvMagsJob hpSrvMagsJob, NormalizationJob normalizationJob,
                           EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob) {
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.hpSrvMagsJob = hpSrvMagsJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startPdcHazardImport() {
        if (Boolean.parseBoolean(hpSrvImportEnabled)) {
            hpSrvSearchJob.run();
        } else {
            LOG.info("HpSrv import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvMagsImport.initialDelay}", fixedRateString = "${scheduler.hpSrvMagsImport.fixedDelay}")
    public void startPdcMagsImport() {
        if (Boolean.parseBoolean(hpSrvMagsImportEnabled)) {
            hpSrvMagsJob.run();
        } else {
            LOG.info("HpSrv mags import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.normalization.initialDelay}", fixedRateString = "${scheduler.normalization.fixedDelay}")
    public void startNormalization() {
        if (Boolean.parseBoolean(normalizationEnabled)) {
            normalizationJob.run();
        } else {
            LOG.info("Normalization job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.eventCombination.initialDelay}", fixedRateString = "${scheduler.eventCombination.fixedDelay}")
    public void startCombinationJob() {
        if (Boolean.parseBoolean(eventCombinationEnabled)) {
            eventCombinationJob.run();
        } else {
            LOG.info("Combination job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.feedComposition.initialDelay}", fixedRateString = "${scheduler.feedComposition.fixedDelay}")
    public void startFeedCompositionJob() {
        if (Boolean.parseBoolean(feedCompositionEnabled)) {
            feedCompositionJob.run();
        } else {
            LOG.info("Feed Compose job invocation is skipped");
        }
    }
}
