package io.kontur.eventapi.config;

import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WorkerSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private WorkerScheduler scheduler;

    @MockBean
    private HpSrvSearchJob hpSrvSearchJob;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(hpSrvSearchJob);
    }

//    @Test
    public void startHpSrvSearchJob() throws InterruptedException {
        ReflectionTestUtils.setField(scheduler, "hpSrvImportEnabled", "true");
        scheduler.startPdcHazardImport();

        Thread.sleep(10);

        verify(hpSrvSearchJob, times(1)).run();
    }

//    @Test
    public void skipHpSrvSearchJobInvocation() throws InterruptedException {
        ReflectionTestUtils.setField(scheduler, "hpSrvImportEnabled", "false");
        scheduler.startPdcHazardImport();

        Thread.sleep(10);

        verify(hpSrvSearchJob, times(0)).run();
    }

}