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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        when(dataLakeDao.getLatestDataLakeByExternalIdAndProvider(any(), any())).thenReturn(Optional.empty());
        when(nifcDataLakeConverter.convertDataLake(any(), any(), any(), any())).thenReturn(new DataLake());
        doNothing().when(dataLakeDao).storeDataLakes(any());

        nifcImportJob.execute();

        verify(nifcClient, times(1)).getNifcLocations();
        verify(nifcClient, times(1)).getNifcPerimeters();
        verify(dataLakeDao, times(2)).getLatestDataLakeByExternalIdAndProvider(any(), any());
        verify(nifcDataLakeConverter, times(2)).convertDataLake(any(), any(), any(), any());
        verify(dataLakeDao, times(2)).storeDataLakes(any());
    }

    private String readFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }
}