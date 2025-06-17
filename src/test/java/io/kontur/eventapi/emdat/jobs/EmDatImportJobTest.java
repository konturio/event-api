package io.kontur.eventapi.emdat.jobs;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.emdat.dto.EmDatPublicFile;
import io.kontur.eventapi.emdat.service.EmDatImportService;
import io.kontur.eventapi.entity.DataLake;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Collections;

import static io.kontur.eventapi.emdat.jobs.EmDatImportJob.EM_DAT_PROVIDER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmDatImportJobTest {

    @Mock
    private EmDatImportService importService;
    @Mock
    private DataLakeDao dataLakeDao;

    @AfterEach
    void resetMocks() {
        reset(importService);
        reset(dataLakeDao);
    }

    @Test
    void execute() throws Exception {
        when(importService.obtainAuthToken()).thenReturn("token");
        EmDatPublicFile file = new EmDatPublicFile();
        file.setName("em-dat.test1.xlsx");
        when(importService.obtainFileStatistic("token")).thenReturn(file);
        InputStream is = getClass().getResourceAsStream("em-dat.test1.xlsx");
        when(importService.obtainFile("em-dat.test1.xlsx", "token")).thenReturn(is);
        when(dataLakeDao.getDataLakesByExternalIdsAndProvider(anySet(), eq(EM_DAT_PROVIDER)))
                .thenReturn(Collections.emptyList());

        EmDatImportJob job = new EmDatImportJob(new SimpleMeterRegistry(), importService, dataLakeDao);
        job.run();

        verify(importService, times(1)).obtainAuthToken();
        verify(importService, times(1)).obtainFileStatistic("token");
        verify(importService, times(1)).obtainFile("em-dat.test1.xlsx", "token");
        verify(dataLakeDao, times(1)).getDataLakesByExternalIdsAndProvider(anySet(), eq(EM_DAT_PROVIDER));
        verify(dataLakeDao, times(3)).storeEventData(any(DataLake.class));
        verify(dataLakeDao, never()).getDataLakesByExternalId(anyString());
    }
}
