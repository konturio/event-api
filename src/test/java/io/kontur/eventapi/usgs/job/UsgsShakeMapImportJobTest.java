package io.kontur.eventapi.usgs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.usgs.client.UsgsClient;
import io.kontur.eventapi.usgs.converter.UsgsShakeMapDataLakeConverter;
import io.kontur.eventapi.usgs.service.UsgsShakeMapService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UsgsShakeMapImportJobTest {

    private final UsgsClient client = mock(UsgsClient.class);
    private final DataLakeDao dataLakeDao = mock(DataLakeDao.class);
    private final UsgsShakeMapDataLakeConverter converter = mock(UsgsShakeMapDataLakeConverter.class);
    private final UsgsShakeMapService service = new UsgsShakeMapService(client, dataLakeDao, converter);
    private final UsgsShakeMapImportJob job = new UsgsShakeMapImportJob(new SimpleMeterRegistry(), service);

    @AfterEach
    void resetMocks() {
        reset(client, dataLakeDao, converter);
    }

    @Test
    void testExecute() throws IOException {
        String feed = readFile("UsgsShakeMapImportJob_Feed.json");
        String detail = readFile("UsgsShakeMapImportJob_Detail.json");
        when(client.getShakeMapEvents(anyString(), anyString(), anyString(), anyInt(), any())).thenReturn(feed);
        when(client.getEvent(anyString(), anyString(), anyString())).thenReturn(detail);
        when(dataLakeDao.getDataLakesByExternalIdsAndProvider(anySet(), anyString())).thenReturn(Collections.emptyList());
        when(converter.convertDataLake(any(), any(), any())).thenReturn(new DataLake());

        job.execute();

        verify(client, times(1)).getShakeMapEvents("geojson", "time", "shakemap", 20, 4.5);
        verify(client, times(1)).getEvent("test1", "geojson", "shakemap");
        verify(dataLakeDao, times(1)).storeDataLakes(any());
    }

    private String readFile(String name) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(name), StandardCharsets.UTF_8);
    }
}
