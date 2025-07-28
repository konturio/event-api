package io.kontur.eventapi.usgs.earthquake.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.usgs.earthquake.client.UsgsEarthquakeClient;
import io.kontur.eventapi.usgs.earthquake.converter.UsgsEarthquakeDataLakeConverter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsgsEarthquakeImportJobTest {

    @Mock
    private UsgsEarthquakeClient client;
    @Mock
    private DataLakeDao dataLakeDao;
    @Mock
    private UsgsEarthquakeDataLakeConverter converter;

    @AfterEach
    void resetMocks() {
        reset(client);
        reset(dataLakeDao);
        reset(converter);
    }

    @Test
    void execute() {
        Feature feature = new Feature("id1", new Point(new double[]{0,0}), Map.of("updated", "123"));
        FeatureCollection fc = new FeatureCollection(new Feature[]{feature});
        when(client.getEarthquakes()).thenReturn(fc.toString());
        when(dataLakeDao.isNewEvent(anyString(), anyString(), anyString())).thenReturn(true);
        when(converter.convert(anyString(), any(), anyString())).thenReturn(new DataLake());
        doNothing().when(dataLakeDao).storeDataLakes(anyList());

        UsgsEarthquakeImportJob job = new UsgsEarthquakeImportJob(new SimpleMeterRegistry(), client,
                dataLakeDao, converter);
        job.run();

        verify(client, times(1)).getEarthquakes();
        verify(dataLakeDao, times(1)).isNewEvent(anyString(), anyString(), anyString());
        verify(converter, times(1)).convert(anyString(), any(), anyString());
        verify(dataLakeDao, times(1)).storeDataLakes(anyList());
    }

    @Test
    void skipExistingEvent() {
        Feature feature = new Feature("id1", new Point(new double[]{0,0}), Map.of("updated", "123"));
        FeatureCollection fc = new FeatureCollection(new Feature[]{feature});
        when(client.getEarthquakes()).thenReturn(fc.toString());
        when(dataLakeDao.isNewEvent(anyString(), anyString(), anyString())).thenReturn(false);

        UsgsEarthquakeImportJob job = new UsgsEarthquakeImportJob(new SimpleMeterRegistry(), client,
                dataLakeDao, converter);
        job.run();

        verify(client, times(1)).getEarthquakes();
        verify(dataLakeDao, times(1)).isNewEvent(anyString(), anyString(), anyString());
        verify(converter, never()).convert(anyString(), any(), anyString());
        verify(dataLakeDao, never()).storeDataLakes(anyList());
    }
}
