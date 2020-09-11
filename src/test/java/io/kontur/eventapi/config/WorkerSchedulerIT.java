package io.kontur.eventapi.config;

import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

class WorkerSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private WorkerScheduler scheduler;

    @MockBean
    private HpSrvSearchJob hpSrvSearchJob;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(hpSrvSearchJob);
    }

    @Test
    public void startHpSrvSearchJob() {
        ReflectionTestUtils.setField(scheduler, "hpSrvImportEnabled", "true");
        scheduler.startPdcHazardImport();

        await()
                .atMost(1, TimeUnit.SECONDS)
                .ignoreException(WantedButNotInvoked.class)
                .until(() -> {
                    verify(hpSrvSearchJob, times(1)).run();
                    return true;
                });
    }

    @Test
    public void skipHpSrvSearchJobInvocation() throws InterruptedException {
        ReflectionTestUtils.setField(scheduler, "hpSrvImportEnabled", "false");
        scheduler.startPdcHazardImport();

        Thread.sleep(100);

        verify(hpSrvSearchJob, never()).run();
    }

}