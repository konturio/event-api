package io.kontur.eventapi.calfire.job;

import io.kontur.eventapi.calfire.client.CalFireClient;
import io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalFireSearchJobTest {
    @Mock
    private DataLakeDao dataLakeDao;

    @Mock
    private CalFireClient calFireClient;

    @Mock
    private CalFireDataLakeConverter calFireDataLakeConverter;

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
        reset(calFireClient);
        reset(calFireDataLakeConverter);
    }

    @Test
    public void testCalFireSearchJob() throws Exception {
        //given
        prepareMocks(2);

        //when
        CalFireSearchJob calFireSearchJob = new CalFireSearchJob(
                new SimpleMeterRegistry(), calFireClient, calFireDataLakeConverter, dataLakeDao);
        calFireSearchJob.run();

        //then
        verify(calFireClient, times(1)).getEvents();
        verify(calFireDataLakeConverter, times(2))
                .convertEvent(isA(String.class), isA(String.class), isA(OffsetDateTime.class));
        verify(dataLakeDao, times(2)).isNewEvent(isA(String.class), isA(String.class), isA(String.class));
        verify(dataLakeDao, times(1)).storeDataLakes(anyList());
    }

    @Test
    public void testCalFireSearchJobWithoutEvents() throws Exception {
        //given
        prepareMocks(0);

        //when
        CalFireSearchJob calFireSearchJob = new CalFireSearchJob(
                new SimpleMeterRegistry(), calFireClient, calFireDataLakeConverter, dataLakeDao);
        calFireSearchJob.run();

        //then
        verify(calFireClient, times(1)).getEvents();
        verify(calFireDataLakeConverter, times(0))
                .convertEvent(isA(String.class), isA(String.class), isA(OffsetDateTime.class));
        verify(dataLakeDao, times(0))
                .isNewEvent(isA(String.class), isA(String.class), isA(String.class));
        verify(dataLakeDao, times(0)).storeDataLakes(anyList());
    }

    private void prepareMocks(int eventsCount) throws Exception {
        Feature[] features = new Feature[eventsCount];
        for (int i = 0; i < eventsCount; i++) {
            features[i] = new Feature(new Point(new double[]{0, 0}), Map.of(
                    "UniqueId", UUID.randomUUID().toString(),
                    "Updated", "2021-01-02T03:04:05Z"));
        }
        String events = new FeatureCollection(features).toString();

        when(calFireClient.getEvents()).thenReturn(events);
        when(calFireDataLakeConverter.convertEvent(isA(String.class), isA(String.class), isA(OffsetDateTime.class)))
                .thenReturn(new DataLake());
        when(dataLakeDao.isNewEvent(isA(String.class), isA(String.class), isA(String.class))).thenReturn(true);
        doNothing().when(dataLakeDao).storeDataLakes(anyList());
    }
}