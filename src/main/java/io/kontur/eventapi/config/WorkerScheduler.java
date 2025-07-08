package io.kontur.eventapi.config;

import static io.kontur.eventapi.entity.PdcMapSrvSearchJobs.PDC_MAP_SRV_IDS;

import java.util.concurrent.CompletableFuture;

import io.kontur.eventapi.calfire.job.CalFireSearchJob;
import io.kontur.eventapi.entity.PdcMapSrvSearchJobs;
import io.kontur.eventapi.firms.eventcombination.FirmsEventCombinationJob;
import io.kontur.eventapi.firms.jobs.FirmsImportModisJob;
import io.kontur.eventapi.firms.jobs.FirmsImportNoaaJob;
import io.kontur.eventapi.firms.jobs.FirmsImportSuomiJob;
import io.kontur.eventapi.inciweb.job.InciWebImportJob;
import io.kontur.eventapi.job.*;
import io.kontur.eventapi.nhc.job.NhcAtImportJob;
import io.kontur.eventapi.nhc.job.NhcCpImportJob;
import io.kontur.eventapi.nhc.job.NhcEpImportJob;
import io.kontur.eventapi.nifc.job.NifcImportJob;
import io.kontur.eventapi.pdc.job.PdcMapSrvSearchJob;
import io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob;
import io.kontur.eventapi.usgs.earthquake.job.UsgsEarthquakeImportJob;
import io.kontur.eventapi.staticdata.job.StaticImportJob;
import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import io.kontur.eventapi.pdc.job.HpSrvMagsJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.tornadojapanma.job.HistoricalTornadoJapanMaImportJob;
import io.kontur.eventapi.tornadojapanma.job.TornadoJapanMaImportJob;
import io.kontur.eventapi.uhc.job.HumanitarianCrisisImportJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!pipelineDisabled")
public class WorkerScheduler {

    private final HpSrvSearchJob hpSrvSearchJob;
    private final HpSrvMagsJob hpSrvMagsJob;
    private final GdacsSearchJob gdacsSearchJob;
    private final FirmsImportModisJob firmsImportModisJob;
    private final FirmsImportNoaaJob firmsImportNoaaJob;
    private final FirmsImportSuomiJob firmsImportSuomiJob;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FirmsEventCombinationJob firmsEventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final EmDatImportJob emDatImportJob;
    private final StaticImportJob staticImportJob;
    private final StormsNoaaImportJob stormsNoaaImportJob;
    private final TornadoJapanMaImportJob tornadoJapanMaImportJob;
    private final HistoricalTornadoJapanMaImportJob historicalTornadoJapanMaImportJob;
    private final PdcMapSrvSearchJobs pdcMapSrvSearchJobs;
    private final EnrichmentJob enrichmentJob;
    private final CalFireSearchJob calFireSearchJob;
    private final NifcImportJob nifcImportJob;
    private final UsgsEarthquakeImportJob usgsEarthquakeImportJob;
    private final InciWebImportJob inciWebImportJob;
    private final HumanitarianCrisisImportJob humanitarianCrisisImportJob;
    private final MetricsJob metricsJob;
    private final ReEnrichmentJob reEnrichmentJob;
    private final NhcAtImportJob nhcAtImportJob;
    private final NhcCpImportJob nhcCpImportJob;
    private final NhcEpImportJob nhcEpImportJob;
    private final EventExpirationJob eventExpirationJob;

    @Value("${scheduler.hpSrvImport.enable}")
    private String hpSrvImportEnabled;
    @Value("${scheduler.hpSrvMagsImport.enable}")
    private String hpSrvMagsImportEnabled;
    @Value("${scheduler.gdacsImport.enable}")
    private String gdacsImportEnabled;
    @Value("${scheduler.firmsModisImport.enable}")
    private String firmsModisImportEnabled;
    @Value("${scheduler.firmsNoaaImport.enable}")
    private String firmsNoaaImportEnabled;
    @Value("${scheduler.firmsSuomiImport.enable}")
    private String firmsSuomiImportEnabled;
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
    @Value("${scheduler.nifcImport.enable}")
    private String nifcImportEnabled;
    @Value("${scheduler.usgsEarthquakeImport.enable}")
    private String usgsEarthquakeImportEnabled;
    @Value("${scheduler.inciwebImport.enable}")
    private String inciwebEnabled;
    @Value("${scheduler.humanitarianCrisisImport.enable}")
    private String humanitarianCrisisEnabled;
    @Value("${scheduler.nhcAtImport.enable}")
    private String nhcAtEnabled;
    @Value("${scheduler.nhcCpImport.enable}")
    private String nhcCpEnabled;
    @Value("${scheduler.nhcEpImport.enable}")
    private String nhcEpEnabled;
    @Value("${scheduler.metrics.enable}")
    private String metricsEnabled;
    @Value("${scheduler.reEnrichment.enable}")
    private String reEnrichmentEnabled;
    @Value("${scheduler.eventExpiration.enable}")
    private String eventExpirationEnabled;


    public WorkerScheduler(HpSrvSearchJob hpSrvSearchJob, HpSrvMagsJob hpSrvMagsJob,
                           GdacsSearchJob gdacsSearchJob, NormalizationJob normalizationJob,
                           EventCombinationJob eventCombinationJob, FirmsEventCombinationJob firmsEventCombinationJob,
                           FeedCompositionJob feedCompositionJob, FirmsImportModisJob firmsImportModisJob,
                           FirmsImportNoaaJob firmsImportNoaaJob, FirmsImportSuomiJob firmsImportSuomiJob,
                           EmDatImportJob emDatImportJob, StaticImportJob staticImportJob,
                           StormsNoaaImportJob stormsNoaaImportJob, TornadoJapanMaImportJob tornadoJapanMaImportJob,
                           HistoricalTornadoJapanMaImportJob historicalTornadoJapanMaImportJob,
                           PdcMapSrvSearchJobs pdcMapSrvSearchJobs,
                           EnrichmentJob enrichmentJob, CalFireSearchJob calFireSearchJob, NifcImportJob nifcImportJob,
                           UsgsEarthquakeImportJob usgsEarthquakeImportJob,
                           InciWebImportJob inciWebImportJob, HumanitarianCrisisImportJob humanitarianCrisisImportJob,
                           NhcAtImportJob nhcAtImportJob, NhcCpImportJob nhcCpImportJob, NhcEpImportJob nhcEpImportJob,
                           MetricsJob metricsJob, ReEnrichmentJob reEnrichmentJob, EventExpirationJob eventExpirationJob) {
        this.hpSrvSearchJob = hpSrvSearchJob;
        this.hpSrvMagsJob = hpSrvMagsJob;
        this.gdacsSearchJob = gdacsSearchJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.firmsEventCombinationJob = firmsEventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.firmsImportModisJob = firmsImportModisJob;
        this.firmsImportNoaaJob = firmsImportNoaaJob;
        this.firmsImportSuomiJob = firmsImportSuomiJob;
        this.emDatImportJob = emDatImportJob;
        this.staticImportJob = staticImportJob;
        this.stormsNoaaImportJob = stormsNoaaImportJob;
        this.tornadoJapanMaImportJob = tornadoJapanMaImportJob;
        this.historicalTornadoJapanMaImportJob = historicalTornadoJapanMaImportJob;
        this.pdcMapSrvSearchJobs = pdcMapSrvSearchJobs;
        this.enrichmentJob = enrichmentJob;
        this.calFireSearchJob = calFireSearchJob;
        this.nifcImportJob = nifcImportJob;
        this.usgsEarthquakeImportJob = usgsEarthquakeImportJob;
        this.inciWebImportJob = inciWebImportJob;
        this.nhcAtImportJob = nhcAtImportJob;
        this.nhcCpImportJob = nhcCpImportJob;
        this.nhcEpImportJob = nhcEpImportJob;
        this.metricsJob = metricsJob;
        this.reEnrichmentJob = reEnrichmentJob;
        this.humanitarianCrisisImportJob = humanitarianCrisisImportJob;
        this.eventExpirationJob = eventExpirationJob;
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startPdcHazardImport() {
        if (Boolean.parseBoolean(hpSrvImportEnabled)) {
            hpSrvSearchJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.hpSrvMagsImport.initialDelay}", fixedDelayString = "${scheduler.hpSrvMagsImport.fixedDelay}")
    public void startPdcMagsImport() {
        if (Boolean.parseBoolean(hpSrvMagsImportEnabled)) {
            hpSrvMagsJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.pdcMapSrvSearch.initialDelay}", fixedDelayString = "${scheduler.pdcMapSrvSearch.fixedDelay}")
    public void startPdcMapSrvSearch() {
        if (Boolean.parseBoolean(pdcMapSrvSearchEnabled)) {
            PdcMapSrvSearchJob[] jobs = new PdcMapSrvSearchJob[PDC_MAP_SRV_IDS.length];
            jobs = pdcMapSrvSearchJobs.getJobs().toArray(jobs);
            for (int i = 0; i < PDC_MAP_SRV_IDS.length; i++) {
                PdcMapSrvSearchJob job = jobs[i];
                String key = PDC_MAP_SRV_IDS[i];
                CompletableFuture.supplyAsync(() -> job.run(key));
            }
        }
    }

    @Scheduled(cron = "${scheduler.gdacsImport.cron}")
    public void startGdacsImport() {
        if (Boolean.parseBoolean(gdacsImportEnabled)) {
            gdacsSearchJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.firmsModisImport.initialDelay}", fixedDelayString= "${scheduler.firmsModisImport.fixedDelay}")
    public void startFirmsModisImport() {
        if (Boolean.parseBoolean(firmsModisImportEnabled)) {
            firmsImportModisJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.firmsNoaaImport.initialDelay}", fixedDelayString= "${scheduler.firmsNoaaImport.fixedDelay}")
    public void startFirmsNoaaImport() {
        if (Boolean.parseBoolean(firmsNoaaImportEnabled)) {
            firmsImportNoaaJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.firmsSuomiImport.initialDelay}", fixedDelayString= "${scheduler.firmsSuomiImport.fixedDelay}")
    public void startFirmsSuomiImport() {
        if (Boolean.parseBoolean(firmsSuomiImportEnabled)) {
            firmsImportSuomiJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.emDatImport.initialDelay}", fixedDelayString = "${scheduler.emDatImport.fixedDelay}")
    public void emDatImportJob() {
        if (Boolean.parseBoolean(emDatImportEnabled)) {
            emDatImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.staticImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startStaticImport() {
        if (Boolean.parseBoolean(staticImportEnabled)) {
            staticImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.stormsNoaaImport.initialDelay}", fixedDelayString = "${scheduler.stormsNoaaImport.fixedDelay}")
    public void startStormNoaaImport() {
        if (Boolean.parseBoolean(stormsNoaaImportEnabled)) {
            stormsNoaaImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.tornadoJapanMaImport.initialDelay}", fixedDelayString = "${scheduler.tornadoJapanMaImport.fixedDelay}")
    public void startTornadoJapanMaImport() {
        if (Boolean.parseBoolean(tornadoJapanMaImportEnabled)) {
            tornadoJapanMaImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.historicalTornadoJapanMaImport.initialDelay}", fixedDelay = Integer.MAX_VALUE)
    public void startHistoricalTornadoJapanMaImport() {
        if (Boolean.parseBoolean(historicalTornadoJapanMaImportEnabled)) {
            historicalTornadoJapanMaImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.calfireSearch.initialDelay}", fixedDelayString = "${scheduler.calfireSearch.fixedDelay}")
    public void startCalFireSearchJob() {
        if (Boolean.parseBoolean(calfireEnabled)) {
            calFireSearchJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.nifcImport.initialDelay}", fixedDelayString = "${scheduler.nifcImport.fixedDelay}")
    public void startNifcImport() {
        if (Boolean.parseBoolean(nifcImportEnabled)) {
            nifcImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.usgsEarthquakeImport.initialDelay}", fixedDelayString = "${scheduler.usgsEarthquakeImport.fixedDelay}")
    public void startUsgsEarthquakeImport() {
        if (Boolean.parseBoolean(usgsEarthquakeImportEnabled)) {
            usgsEarthquakeImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.inciwebImport.initialDelay}", fixedDelayString = "${scheduler.inciwebImport.fixedDelay}")
    public void startInciWebImport() {
        if (Boolean.parseBoolean(inciwebEnabled)) {
            inciWebImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.humanitarianCrisisImport.initialDelay}",
            fixedDelayString = "${scheduler.humanitarianCrisisImport.fixedDelay}")
    public void startHumanitarianCrisisImport() {
        if (Boolean.parseBoolean(humanitarianCrisisEnabled)) {
            humanitarianCrisisImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.nhcAtImport.initialDelay}", fixedDelayString = "${scheduler.nhcAtImport.fixedDelay}")
    public void startNhcAtImport() {
        if (Boolean.parseBoolean(nhcAtEnabled)) {
            nhcAtImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.nhcCpImport.initialDelay}", fixedDelayString = "${scheduler.nhcCpImport.fixedDelay}")
    public void startNhcCpImport() {
        if (Boolean.parseBoolean(nhcCpEnabled)) {
            nhcCpImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.nhcEpImport.initialDelay}", fixedDelayString = "${scheduler.nhcEpImport.fixedDelay}")
    public void startNhcEpImport() {
        if (Boolean.parseBoolean(nhcEpEnabled)) {
            nhcEpImportJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.normalization.initialDelay}", fixedDelayString = "${scheduler.normalization.fixedDelay}")
    public void startNormalization() {
        if (Boolean.parseBoolean(normalizationEnabled)) {
            normalizationJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.eventCombination.initialDelay}", fixedDelayString = "${scheduler.eventCombination.fixedDelay}")
    public void startCombinationJob() {
        if (Boolean.parseBoolean(eventCombinationEnabled)) {
            eventCombinationJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.eventCombination.initialDelay}", fixedDelayString = "${scheduler.eventCombination.fixedDelay}")
    public void startFirmsCombinationJob() {
        if (Boolean.parseBoolean(eventCombinationEnabled)) {
            firmsEventCombinationJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.feedComposition.initialDelay}", fixedDelayString = "${scheduler.feedComposition.fixedDelay}")
    public void startFeedCompositionJob() {
        if (Boolean.parseBoolean(feedCompositionEnabled)) {
            feedCompositionJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.enrichment.initialDelay}", fixedDelayString = "${scheduler.enrichment.fixedDelay}")
    public void startEnrichmentJob() {
        if (Boolean.parseBoolean(enrichmentEnabled)) {
            enrichmentJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.reEnrichment.initialDelay}", fixedDelayString = "${scheduler.reEnrichment.fixedDelay}")
    public void startReEnrichmentJob() {
        if (Boolean.parseBoolean(reEnrichmentEnabled)) {
            reEnrichmentJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.metrics.initialDelay}", fixedDelayString = "${scheduler.metrics.fixedDelay}")
    public void startMetricsJob() {
        if (Boolean.parseBoolean(metricsEnabled)) {
            metricsJob.run();
        }
    }

    @Scheduled(initialDelayString = "${scheduler.eventExpiration.initialDelay}", fixedDelayString = "${scheduler.eventExpiration.fixedDelay}")
    public void startExpirationJob() {
        if (Boolean.parseBoolean(eventExpirationEnabled)) {
            eventExpirationJob.run();
        }
    }
}
