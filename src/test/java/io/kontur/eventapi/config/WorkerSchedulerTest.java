package io.kontur.eventapi.config;

import io.kontur.eventapi.emdat.jobs.EmDatImportJob;
import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob;
import io.kontur.eventapi.pdc.job.HpSrvMagsJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.firms.jobs.FirmsImportJob;
import io.kontur.eventapi.staticdata.job.StaticImportJob;
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
    private final FirmsImportJob firmsImportJob = mock(FirmsImportJob.class);
    private final EmDatImportJob emDatImportJob = mock(EmDatImportJob.class);
    private final StaticImportJob staticImportJob = mock(StaticImportJob.class);
    private final StormsNoaaImportJob stormsNoaaImportJob = mock(StormsNoaaImportJob.class);

    private final WorkerScheduler scheduler = new WorkerScheduler(hpSrvSearchJob, hpSrvMagsJob, gdacsSearchJob, normalizationJob, eventCombinationJob,
            feedCompositionJob, firmsImportJob, emDatImportJob, staticImportJob, stormsNoaaImportJob);

    @AfterEach
    public void resetMocks() {
        Mockito.reset(hpSrvSearchJob);
        Mockito.reset(hpSrvMagsJob);
        Mockito.reset(gdacsSearchJob);
        Mockito.reset(firmsImportJob);
        Mockito.reset(staticImportJob);
        Mockito.reset(normalizationJob);
        Mockito.reset(eventCombinationJob);
        Mockito.reset(feedCompositionJob);
        Mockito.reset(stormsNoaaImportJob);
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
    public void startGdacsSearchJob() {
        ReflectionTestUtils.setField(scheduler, "gdacsImportEnabled", "true");
        scheduler.startGdacsImport();

        verify(gdacsSearchJob, times(1)).run();
    }

    @Test
    public void startFirmsSearchJob() {
        ReflectionTestUtils.setField(scheduler, "firmsImportEnabled", "true");
        scheduler.startFirmsImport();

        verify(firmsImportJob, times(1)).run();
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

}