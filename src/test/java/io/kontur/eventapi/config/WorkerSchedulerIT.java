package io.kontur.eventapi.config;

import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WorkerSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private WorkerScheduler scheduler;

    @MockBean
    private HpSrvSearchJob hpSrvSearchJob;

    @Test
    public void startHpSrvSearchJob() {
        scheduler.startPdcHazardImport();

        verify(hpSrvSearchJob, times(1)).run();
    }

}