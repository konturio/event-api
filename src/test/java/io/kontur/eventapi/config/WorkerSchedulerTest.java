package io.kontur.eventapi.config;

import io.kontur.eventapi.calfire.job.CalFireSearchJob;
import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.firms.eventcombination.FirmsEventCombinationJob;
import io.kontur.eventapi.firms.jobs.FirmsImportModisJob;
import io.kontur.eventapi.firms.jobs.FirmsImportNoaaJob;
import io.kontur.eventapi.firms.jobs.FirmsImportSuomiJob;
import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import io.kontur.eventapi.inciweb.job.InciWebImportJob;
import io.kontur.eventapi.job.EnrichmentJob;
import io.kontur.eventapi.job.ReEnrichmentJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.firms.episodecomposition.FirmsFeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.job.MetricsJob;
import io.kontur.eventapi.nifc.job.NifcImportJob;
import io.kontur.eventapi.pdc.job.PdcMapSrvSearchJob;
import io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob;
import io.kontur.eventapi.pdc.job.HpSrvMagsJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.staticdata.job.StaticImportJob;
import io.kontur.eventapi.tornadojapanma.job.HistoricalTornadoJapanMaImportJob;
import io.kontur.eventapi.tornadojapanma.job.TornadoJapanMaImportJob;
import io.kontur.eventapi.uhc.job.HumanitarianCrisisImportJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class WorkerSchedulerTest {

    private final HpSrvSearchJob hpSrvSearchJob = mock(HpSrvSearchJob.class);
    private final HpSrvMagsJob hpSrvMagsJob = mock(HpSrvMagsJob.class);
    private final NormalizationJob normalizationJob = mock(NormalizationJob.class);
    private final EventCombinationJob eventCombinationJob = mock(EventCombinationJob.class);
    private final FeedCompositionJob feedCompositionJob = mock(FeedCompositionJob.class);
    private final GdacsSearchJob gdacsSearchJob = mock(GdacsSearchJob.class);
    private final FirmsImportModisJob firmsImportModisJob = mock(FirmsImportModisJob.class);
    private final FirmsImportNoaaJob firmsImportNoaaJob = mock(FirmsImportNoaaJob.class);
    private final FirmsImportSuomiJob firmsImportSuomiJob = mock(FirmsImportSuomiJob.class);
    private final EmDatImportJob emDatImportJob = mock(EmDatImportJob.class);
    private final StaticImportJob staticImportJob = mock(StaticImportJob.class);
    private final StormsNoaaImportJob stormsNoaaImportJob = mock(StormsNoaaImportJob.class);
    private final TornadoJapanMaImportJob tornadoJapanMaImportJob = mock(TornadoJapanMaImportJob.class);
    private final HistoricalTornadoJapanMaImportJob historicalTornadoJapanMaImportJob = mock(HistoricalTornadoJapanMaImportJob.class);
    private final PdcMapSrvSearchJob pdcMapSrvSearchJob = mock(PdcMapSrvSearchJob.class);
    private final FirmsFeedCompositionJob firmsFeedCompositionJob = mock(FirmsFeedCompositionJob.class);
    private final EnrichmentJob enrichmentJob = mock(EnrichmentJob.class);
    private final FirmsEventCombinationJob firmsEventCombinationJob = mock(FirmsEventCombinationJob.class);
    private final CalFireSearchJob calFireSearchJob = mock(CalFireSearchJob.class);
    private final NifcImportJob nifcImportJob = mock(NifcImportJob.class);
    private final InciWebImportJob inciWebImportJob = mock(InciWebImportJob.class);
    private final HumanitarianCrisisImportJob humanitarianCrisisImportJob = mock(HumanitarianCrisisImportJob.class);
    private final MetricsJob metricsJob = mock(MetricsJob.class);
    private final ReEnrichmentJob reEnrichmentJob = mock(ReEnrichmentJob.class);

    private final WorkerScheduler scheduler = new WorkerScheduler(hpSrvSearchJob, hpSrvMagsJob, gdacsSearchJob, normalizationJob,
            eventCombinationJob, firmsEventCombinationJob, feedCompositionJob, firmsImportModisJob, firmsImportNoaaJob,
            firmsImportSuomiJob, emDatImportJob, staticImportJob, stormsNoaaImportJob, tornadoJapanMaImportJob,
            historicalTornadoJapanMaImportJob, pdcMapSrvSearchJob, firmsFeedCompositionJob, enrichmentJob, calFireSearchJob,
            nifcImportJob, inciWebImportJob, humanitarianCrisisImportJob, metricsJob, reEnrichmentJob);

    @AfterEach
    public void resetMocks() {
        Mockito.reset(hpSrvSearchJob);
        Mockito.reset(hpSrvMagsJob);
        Mockito.reset(gdacsSearchJob);
        Mockito.reset(firmsImportModisJob);
        Mockito.reset(firmsImportNoaaJob);
        Mockito.reset(firmsImportSuomiJob);
        Mockito.reset(staticImportJob);
        Mockito.reset(normalizationJob);
        Mockito.reset(eventCombinationJob);
        Mockito.reset(feedCompositionJob);
        Mockito.reset(stormsNoaaImportJob);
        Mockito.reset(tornadoJapanMaImportJob);
        Mockito.reset(historicalTornadoJapanMaImportJob);
        Mockito.reset(pdcMapSrvSearchJob);
        Mockito.reset(calFireSearchJob);
        Mockito.reset(inciWebImportJob);
        Mockito.reset(reEnrichmentJob);
    }

    @Test
    public void startHpSrvSearchJob() {
        ReflectionTestUtils.setField(scheduler, "hpSrvImportEnabled", "true");
        scheduler.startPdcHazardImport();

        verify(hpSrvSearchJob, times(1)).run();
    }

    @Test
    public void skipHpSrvSearchJobInvocation() {
        ReflectionTestUtils.setField(scheduler, "hpSrvImportEnabled", "false");
        scheduler.startPdcHazardImport();

        verify(hpSrvSearchJob, never()).run();
    }

    @Test
    public void startHpSrvMagsImportJob() {
        ReflectionTestUtils.setField(scheduler, "hpSrvMagsImportEnabled", "true");
        scheduler.startPdcMagsImport();

        verify(hpSrvMagsJob, times(1)).run();
    }

    @Test
    public void skipHpSrvMagsImportJob() {
        ReflectionTestUtils.setField(scheduler, "hpSrvMagsImportEnabled", "false");
        scheduler.startPdcMagsImport();

        verify(hpSrvMagsJob, never()).run();
    }

    @Test
    public void startPdcMapSrvSearchJob() {
        ReflectionTestUtils.setField(scheduler, "pdcMapSrvSearchEnabled", "true");
        scheduler.startPdcMapSrvSearch();

        verify(pdcMapSrvSearchJob, times(1)).run();
    }

    @Test
    public void skipPdcMapSrvSearchJob() {
        ReflectionTestUtils.setField(scheduler, "pdcMapSrvSearchEnabled", "false");
        scheduler.startPdcMapSrvSearch();

        verify(pdcMapSrvSearchJob, never()).run();
    }

    @Test
    public void startGdacsSearchJob() {
        ReflectionTestUtils.setField(scheduler, "gdacsImportEnabled", "true");
        scheduler.startGdacsImport();

        verify(gdacsSearchJob, times(1)).run();
    }

    @Test
    public void startFirmsModisSearchJob() {
        ReflectionTestUtils.setField(scheduler, "firmsModisImportEnabled", "true");
        scheduler.startFirmsModisImport();

        verify(firmsImportModisJob, times(1)).run();
    }

    @Test
    public void startFirmsNoaaSearchJob() {
        ReflectionTestUtils.setField(scheduler, "firmsNoaaImportEnabled", "true");
        scheduler.startFirmsNoaaImport();

        verify(firmsImportNoaaJob, times(1)).run();
    }

    @Test
    public void startFirmsSuomiSearchJob() {
        ReflectionTestUtils.setField(scheduler, "firmsSuomiImportEnabled", "true");
        scheduler.startFirmsSuomiImport();

        verify(firmsImportSuomiJob, times(1)).run();
    }

    @Test
    public void startStaticImportJob() {
        ReflectionTestUtils.setField(scheduler, "staticImportEnabled", "true");
        scheduler.startStaticImport();

        verify(staticImportJob, times(1)).run();
    }

    @Test
    public void skipStaticImportJob() {
        ReflectionTestUtils.setField(scheduler, "staticImportEnabled", "false");
        scheduler.startStaticImport();

        verify(staticImportJob, never()).run();
    }

    @Test
    public void startStormsNoaaImportJob() {
        ReflectionTestUtils.setField(scheduler, "stormsNoaaImportEnabled", "true");
        scheduler.startStormNoaaImport();

        verify(stormsNoaaImportJob, times(1)).run();
    }

    @Test
    public void skipStormsNoaaImportJob() {
        ReflectionTestUtils.setField(scheduler, "stormsNoaaImportEnabled", "false");
        scheduler.startStormNoaaImport();

        verify(stormsNoaaImportJob, never()).run();
    }

    @Test
    public void startHistoricalTornadoJapanMaImportJob() {
        ReflectionTestUtils.setField(scheduler, "historicalTornadoJapanMaImportEnabled", "true");
        scheduler.startHistoricalTornadoJapanMaImport();

        verify(historicalTornadoJapanMaImportJob, times(1)).run();
    }

    @Test
    public void skipHistoricalTornadoJapanMaImportJob() {
        ReflectionTestUtils.setField(scheduler, "historicalTornadoJapanMaImportEnabled", "false");
        scheduler.startHistoricalTornadoJapanMaImport();

        verify(historicalTornadoJapanMaImportJob, never()).run();
    }

    @Test
    public void startTornadoJapanMaImportJob() {
        ReflectionTestUtils.setField(scheduler, "tornadoJapanMaImportEnabled", "true");
        scheduler.startTornadoJapanMaImport();

        verify(tornadoJapanMaImportJob, times(1)).run();
    }

    @Test
    public void skipTornadoJapanMaImportJob() {
        ReflectionTestUtils.setField(scheduler, "tornadoJapanMaImportEnabled", "false");
        scheduler.startTornadoJapanMaImport();

        verify(tornadoJapanMaImportJob, never()).run();
    }

    @Test
    public void startNormalizationJob() {
        ReflectionTestUtils.setField(scheduler, "normalizationEnabled", "true");
        scheduler.startNormalization();

        verify(normalizationJob, times(1)).run();
    }

    @Test
    public void skipNormalizationJob() {
        ReflectionTestUtils.setField(scheduler, "normalizationEnabled", "false");
        scheduler.startNormalization();

        verify(normalizationJob, never()).run();
    }

    @Test
    public void startEventCombinationJob() {
        ReflectionTestUtils.setField(scheduler, "eventCombinationEnabled", "true");
        scheduler.startCombinationJob();

        verify(eventCombinationJob, times(1)).run();
    }

    @Test
    public void skipEventCombinationJob() {
        ReflectionTestUtils.setField(scheduler, "eventCombinationEnabled", "false");
        scheduler.startCombinationJob();

        verify(eventCombinationJob, never()).run();
    }

    @Test
    public void startFeedCompositionJob() {
        ReflectionTestUtils.setField(scheduler, "feedCompositionEnabled", "true");
        scheduler.startFeedCompositionJob();

        verify(feedCompositionJob, times(1)).run();
    }

    @Test
    public void skipFeedCompositionJob() {
        ReflectionTestUtils.setField(scheduler, "feedCompositionEnabled", "false");
        scheduler.startFeedCompositionJob();

        verify(feedCompositionJob, never()).run();
    }

    @Test
    public void startCalFireSearchJob() {
        ReflectionTestUtils.setField(scheduler, "calfireEnabled", "true");
        scheduler.startCalFireSearchJob();

        verify(calFireSearchJob, times(1)).run();
    }

    @Test
    public void skipCalFireSearchJob() {
        ReflectionTestUtils.setField(scheduler, "calfireEnabled", "false");
        scheduler.startCalFireSearchJob();

        verify(calFireSearchJob, never()).run();
    }

    @Test
    public void startInciWebImportJob() {
        ReflectionTestUtils.setField(scheduler, "inciwebEnabled", "true");
        scheduler.startInciWebImport();

        verify(inciWebImportJob, times(1)).run();
    }

    @Test
    public void skipInciWebImportJob() {
        ReflectionTestUtils.setField(scheduler, "inciwebEnabled", "false");
        scheduler.startInciWebImport();

        verify(inciWebImportJob, never()).run();
    }
}