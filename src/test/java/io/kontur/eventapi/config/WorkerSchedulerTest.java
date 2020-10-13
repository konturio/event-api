package io.kontur.eventapi.config;

import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class WorkerSchedulerTest {

    private final HpSrvSearchJob hpSrvSearchJob = mock(HpSrvSearchJob.class);
    private final NormalizationJob normalizationJob = mock(NormalizationJob.class);
    private final EventCombinationJob eventCombinationJob = mock(EventCombinationJob.class);
    private final FeedCompositionJob feedCompositionJob = mock(FeedCompositionJob.class);
    private final GdacsSearchJob gdacsSearchJob = mock(GdacsSearchJob.class);
    private final WorkerScheduler scheduler = new WorkerScheduler(hpSrvSearchJob, gdacsSearchJob, normalizationJob, eventCombinationJob,
            feedCompositionJob);

    @AfterEach
    public void resetMocks() {
        Mockito.reset(hpSrvSearchJob);
        Mockito.reset(gdacsSearchJob);
        Mockito.reset(normalizationJob);
        Mockito.reset(eventCombinationJob);
        Mockito.reset(feedCompositionJob);
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
    public void startGdacsSearchJob() {
        ReflectionTestUtils.setField(scheduler, "gdacsImportEnabled", "true");
        scheduler.startGdacsImport();

        verify(gdacsSearchJob, times(1)).run();
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