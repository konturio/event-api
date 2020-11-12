package io.kontur.eventapi.config;

import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.EventCombinationJob2;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.pdc.job.HpSrvMagsJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.firms.jobs.FirmsImportJob;
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
    private final GdacsSearchJob gdacsSearchJob;
    private final FirmsImportJob firmsImportJob;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final EventCombinationJob2 eventCombinationJob2;
    private final FeedCompositionJob feedCompositionJob;

    @Value("${scheduler.hpSrvImport.enable}")
    private String hpSrvImportEnabled;
    @Value("${scheduler.hpSrvMagsImport.enable}")
    private String hpSrvMagsImportEnabled;
    @Value("${scheduler.gdacsImport.enable}")
    private String gdacsImportEnabled;
    @Value("${scheduler.firmsImport.enable}")
    private String firmsImportEnabled;
    @Value("${scheduler.normalization.enable}")
    private String normalizationEnabled;
    @Value("${scheduler.eventCombination.enable}")
    private String eventCombinationEnabled;
    @Value("${scheduler.feedComposition.enable}")
    private String feedCompositionEnabled;

    public WorkerScheduler(HpSrvSearchJob hpSrvSearchJob, HpSrvMagsJob hpSrvMagsJob,
                           GdacsSearchJob gdacsSearchJob, NormalizationJob normalizationJob,
                           EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob,
                           FirmsImportJob firmsImportJob, EventCombinationJob2 eventCombinationJob2) {
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.hpSrvMagsJob = hpSrvMagsJob;
        this.gdacsSearchJob = gdacsSearchJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.firmsImportJob = firmsImportJob;
        this.eventCombinationJob2 = eventCombinationJob2;
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

    @Scheduled(cron = "${scheduler.gdacsImport.cron}")
    public void startGdacsImport() {
        if (Boolean.parseBoolean(gdacsImportEnabled)) {
            gdacsSearchJob.run();
        } else {
            LOG.info("Gdacs import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.firmsImport.initialDelay}", fixedRateString = "${scheduler.firmsImport.fixedRate}")
    public void startFirmsImport() {
        if (Boolean.parseBoolean(firmsImportEnabled)) {
            firmsImportJob.run();
        } else {
            LOG.info("Firms import job invocation is skipped");
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
            eventCombinationJob2.run();
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
