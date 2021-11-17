package io.kontur.eventapi.config;

import io.kontur.eventapi.calfire.job.CalFireSearchJob;
import io.kontur.eventapi.firms.episodecomposition.FirmsFeedCompositionJob;
import io.kontur.eventapi.firms.eventcombination.FirmsEventCombinationJob;
import io.kontur.eventapi.job.EnrichmentJob;
import io.kontur.eventapi.pdc.job.PdcMapSrvSearchJob;
import io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob;
import io.kontur.eventapi.staticdata.job.StaticImportJob;
import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.pdc.job.HpSrvMagsJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.firms.jobs.FirmsImportJob;
import io.kontur.eventapi.tornadojapanma.job.HistoricalTornadoJapanMaImportJob;
import io.kontur.eventapi.tornadojapanma.job.TornadoJapanMaImportJob;
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
    private final FirmsEventCombinationJob firmsEventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final FirmsFeedCompositionJob firmsFeedCompositionJob;
    private final EmDatImportJob emDatImportJob;
    private final StaticImportJob staticImportJob;
    private final StormsNoaaImportJob stormsNoaaImportJob;
    private final TornadoJapanMaImportJob tornadoJapanMaImportJob;
    private final HistoricalTornadoJapanMaImportJob historicalTornadoJapanMaImportJob;
    private final PdcMapSrvSearchJob pdcMapSrvSearchJob;
    private final EnrichmentJob enrichmentJob;
    private final CalFireSearchJob calFireSearchJob;

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
    @Value("${scheduler.emDatImport.enable}")
    private String emDatImportEnabled;
    @Value("${scheduler.staticImport.enable}")
    private String staticImportEnabled;
    @Value("${scheduler.stormsNoaaImport.enable}")
    private String stormsNoaaImportEnabled;
    @Value("${scheduler.tornadoJapanMaImport.enable}")
    private String tornadoJapanMaImportEnabled;
    @Value("${scheduler.historicalTornadoJapanMaImport.enable}")
    private String historicalTornadoJapanMaImportEnabled;
    @Value("${scheduler.pdcMapSrvSearch.enable}")
    private String pdcMapSrvSearchEnabled;
    @Value("${scheduler.enrichment.enable}")
    private String enrichmentEnabled;
    @Value("${scheduler.calfireSearch.enable}")
    private String calfireEnabled;

    public WorkerScheduler(HpSrvSearchJob hpSrvSearchJob, HpSrvMagsJob hpSrvMagsJob,
                           GdacsSearchJob gdacsSearchJob, NormalizationJob normalizationJob,
                           EventCombinationJob eventCombinationJob, FirmsEventCombinationJob firmsEventCombinationJob,
                           FeedCompositionJob feedCompositionJob, FirmsImportJob firmsImportJob, EmDatImportJob emDatImportJob,
                           StaticImportJob staticImportJob, StormsNoaaImportJob stormsNoaaImportJob,
                           TornadoJapanMaImportJob tornadoJapanMaImportJob,
                           HistoricalTornadoJapanMaImportJob historicalTornadoJapanMaImportJob,
                           PdcMapSrvSearchJob pdcMapSrvSearchJob, FirmsFeedCompositionJob firmsFeedCompositionJob,
                           EnrichmentJob enrichmentJob, CalFireSearchJob calFireSearchJob) {
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.hpSrvMagsJob = hpSrvMagsJob;
        this.gdacsSearchJob = gdacsSearchJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.firmsEventCombinationJob = firmsEventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.firmsImportJob = firmsImportJob;
        this.emDatImportJob = emDatImportJob;
        this.staticImportJob = staticImportJob;
        this.stormsNoaaImportJob = stormsNoaaImportJob;
        this.tornadoJapanMaImportJob = tornadoJapanMaImportJob;
        this.historicalTornadoJapanMaImportJob = historicalTornadoJapanMaImportJob;
        this.pdcMapSrvSearchJob = pdcMapSrvSearchJob;
        this.firmsFeedCompositionJob = firmsFeedCompositionJob;
        this.enrichmentJob = enrichmentJob;
        this.calFireSearchJob = calFireSearchJob;
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startPdcHazardImport() {
        if (Boolean.parseBoolean(hpSrvImportEnabled)) {
            hpSrvSearchJob.run();
        } else {
            LOG.info("HpSrv import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvMagsImport.initialDelay}", fixedDelayString = "${scheduler.hpSrvMagsImport.fixedDelay}")
    public void startPdcMagsImport() {
        if (Boolean.parseBoolean(hpSrvMagsImportEnabled)) {
            hpSrvMagsJob.run();
        } else {
            LOG.info("HpSrv mags import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.pdcMapSrvSearch.initialDelay}", fixedDelayString = "${scheduler.pdcMapSrvSearch.fixedDelay}")
    public void startPdcMapSrvSearch() {
        if (Boolean.parseBoolean(pdcMapSrvSearchEnabled)) {
            pdcMapSrvSearchJob.run();
        } else {
            LOG.info("PdcMapSrv import job invocation is skipped");
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

    @Scheduled(initialDelayString = "${scheduler.firmsImport.initialDelay}", fixedDelayString= "${scheduler.firmsImport.fixedDelay}")
    public void startFirmsImport() {
        if (Boolean.parseBoolean(firmsImportEnabled)) {
            firmsImportJob.run();
        } else {
            LOG.info("Firms import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.staticImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startStaticImport() {
        if (Boolean.parseBoolean(staticImportEnabled)) {
            staticImportJob.run();
        } else {
            LOG.info("Static import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.stormsNoaaImport.initialDelay}", fixedDelayString = "${scheduler.stormsNoaaImport.fixedDelay}")
    public void startStormNoaaImport() {
        if (Boolean.parseBoolean(stormsNoaaImportEnabled)) {
            stormsNoaaImportJob.run();
        } else {
            LOG.info("StormsNoaa import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.tornadoJapanMaImport.initialDelay}", fixedDelayString = "${scheduler.tornadoJapanMaImport.fixedDelay}")
    public void startTornadoJapanMaImport() {
        if (Boolean.parseBoolean(tornadoJapanMaImportEnabled)) {
            tornadoJapanMaImportJob.run();
        } else {
            LOG.info("TornadoJapanMa import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.historicalTornadoJapanMaImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startHistoricalTornadoJapanMaImport() {
        if (Boolean.parseBoolean(historicalTornadoJapanMaImportEnabled)) {
            historicalTornadoJapanMaImportJob.run();
        } else {
            LOG.info("HistoricalTornadoJapanMa import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.normalization.initialDelay}", fixedDelayString = "${scheduler.normalization.fixedDelay}")
    public void startNormalization() {
        if (Boolean.parseBoolean(normalizationEnabled)) {
            normalizationJob.run();
        } else {
            LOG.info("Normalization job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.eventCombination.initialDelay}", fixedDelayString = "${scheduler.eventCombination.fixedDelay}")
    public void startCombinationJob() {
        if (Boolean.parseBoolean(eventCombinationEnabled)) {
            eventCombinationJob.run();
        } else {
            LOG.info("Combination job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.eventCombination.initialDelay}", fixedDelayString = "${scheduler.eventCombination.fixedDelay}")
    public void startFirmsCombinationJob() {
        if (Boolean.parseBoolean(eventCombinationEnabled)) {
            firmsEventCombinationJob.run();
        } else {
            LOG.info("Firms Combination job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.feedComposition.initialDelay}", fixedDelayString = "${scheduler.feedComposition.fixedDelay}")
    public void startFeedCompositionJob() {
        if (Boolean.parseBoolean(feedCompositionEnabled)) {
            feedCompositionJob.run();
        } else {
            LOG.info("Feed Compose job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.feedComposition.initialDelay}", fixedDelayString = "${scheduler.feedComposition.fixedDelay}")
    public void startFirmsFeedCompositionJob() {
        if (Boolean.parseBoolean(feedCompositionEnabled)) {
            firmsFeedCompositionJob.run();
        } else {
            LOG.info("Firms Feed Compose job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.enrichment.initialDelay}", fixedDelayString = "${scheduler.enrichment.fixedDelay}")
    public void startEnrichmentJob() {
        if (Boolean.parseBoolean(enrichmentEnabled)) {
            enrichmentJob.run();
        } else {
            LOG.info("Enrichment job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.emDatImport.initialDelay}", fixedDelayString = "${scheduler.emDatImport.fixedDelay}")
    public void emDatImportJob() {
        if (Boolean.parseBoolean(emDatImportEnabled)) {
            emDatImportJob.run();
        } else {
            LOG.info("EM-DAT Import job invocation is skipped");
        }
    }

    @Scheduled(initialDelayString = "${scheduler.calfireSearch.initialDelay}", fixedDelayString = "${scheduler.calfireSearch.fixedDelay}")
    public void startCalFireSearchJob() {
        if (Boolean.parseBoolean(calfireEnabled)) {
            calFireSearchJob.run();
        } else {
            LOG.info("Calfire job invocation is skipped");
        }
    }
}
