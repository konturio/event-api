package io.kontur.eventapi.nifc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.nifc.client.NifcClient;
import io.kontur.eventapi.nifc.converter.NifcDataLakeConverter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static org.mockito.Mockito.*;

class NifcImportJobTest {

    private final NifcClient nifcClient = mock(NifcClient.class);
    private final DataLakeDao dataLakeDao = mock(DataLakeDao.class);
    private final NifcDataLakeConverter nifcDataLakeConverter = mock(NifcDataLakeConverter.class);
    private final NifcImportJob nifcImportJob = new NifcImportJob(new SimpleMeterRegistry(), nifcClient,
            dataLakeDao, nifcDataLakeConverter);

    @AfterEach
    public void resetMocks() {
        reset(nifcClient);
        reset(dataLakeDao);
        reset(nifcDataLakeConverter);
    }

    @Test
    void execute() throws Exception {
        String locations = readFile("NifcImportJob_Locations.json");
        String perimeters = readFile("NifcImportJob_Perimeters.json");
        when(nifcClient.getNifcLocations()).thenReturn(locations);
        when(nifcClient.getNifcPerimeters()).thenReturn(perimeters);
        when(dataLakeDao.isNewEvent(any(), any(), any())).thenReturn(true);
        when(nifcDataLakeConverter.convertDataLake(any(), any(), any(), any())).thenReturn(new DataLake());
        doNothing().when(dataLakeDao).storeDataLakes(any());

        nifcImportJob.execute();

        verify(nifcClient, times(1)).getNifcLocations();
        verify(nifcClient, times(1)).getNifcPerimeters();
        verify(dataLakeDao, times(2)).getDataLakesByExternalIdsAndProvider(any(), any());
        verify(nifcDataLakeConverter, times(2)).convertDataLake(any(), any(), any(), any());
        verify(dataLakeDao, times(2)).storeDataLakes(any());
    }

    @Test
    void skipInvalidResponses() throws Exception {
        when(nifcClient.getNifcLocations()).thenReturn(" ");
        when(nifcClient.getNifcPerimeters()).thenReturn(null);

        nifcImportJob.execute();

        verify(dataLakeDao, never()).getDataLakesByExternalIdsAndProvider(any(), any());
        verify(nifcDataLakeConverter, never()).convertDataLake(any(), any(), any(), any());
        verify(dataLakeDao, never()).storeDataLakes(any());
    }

    @Test
    void skipMalformedJson() throws Exception {
        when(nifcClient.getNifcLocations()).thenReturn("{not json}");
        when(nifcClient.getNifcPerimeters()).thenReturn("{\"error\":true}");

        nifcImportJob.execute();

        verify(dataLakeDao, never()).getDataLakesByExternalIdsAndProvider(any(), any());
        verify(nifcDataLakeConverter, never()).convertDataLake(any(), any(), any(), any());
        verify(dataLakeDao, never()).storeDataLakes(any());
    }

    private String readFile(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(this.getClass().getResourceAsStream(fileName)), "UTF-8");
    }
}