package io.kontur.eventapi.pdc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.client.PdcMapSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;


class PdcMapSrvSearchJobTest {

    private final DataLakeDao dataLakeDao = mock(DataLakeDao.class);
    private final PdcMapSrvClient pdcMapSrvClient = mock(PdcMapSrvClient.class);
    private final PdcDataLakeConverter pdcDataLakeConverter = mock(PdcDataLakeConverter.class);
    private final PdcMapSrvSearchJob pdcMapSrvSearchJob = new PdcMapSrvSearchJob(
            new SimpleMeterRegistry(), pdcMapSrvClient, pdcDataLakeConverter, dataLakeDao);

    @AfterEach
    public void resetMocks() {
        reset(dataLakeDao);
        reset(pdcMapSrvClient);
        reset(pdcDataLakeConverter);
    }

    @Test
    public void testPdcMapSrvSearchJob() {
        prepareMocks(10);

        pdcMapSrvSearchJob.run();

        verify(pdcMapSrvClient, times(1)).getExposures();
        verify(pdcDataLakeConverter, times(10)).convertExposure(isA(String.class), isA(String.class));
        verify(dataLakeDao, times(1)).getPdcExposureGeohashes(anySet());
        verify(dataLakeDao, times(1)).storeDataLakes(anyList());
    }

    @Test
    public void testPdcMapSrvSearchJobWhenNoExposures() {
        prepareMocks(0);

        pdcMapSrvSearchJob.run();

        verify(pdcMapSrvClient, times(1)).getExposures();
        verify(pdcDataLakeConverter, times(0)).convertExposure(isA(String.class), isA(String.class));
        verify(dataLakeDao, times(0)).getPdcExposureGeohashes(anySet());
        verify(dataLakeDao, times(0)).storeDataLakes(anyList());
    }

    private void prepareMocks(int exposureCount) {
        Feature[] features = new Feature[exposureCount];
        for (int i = 0; i < exposureCount; i++) {
            features[i] = new Feature(new Point(new double[] {0, 0}), Map.of(
                    "hazard_uuid", UUID.randomUUID().toString(),
                    "geohash", UUID.randomUUID().toString()));
        }
        String exposures = new FeatureCollection(features).toString();

        when(pdcMapSrvClient.getExposures()).thenReturn(exposures);
        when(pdcDataLakeConverter.convertExposure(isA(String.class), isA(String.class))).then(i ->
                new DataLake(null, i.getArgument(1, String.class), null, null));
        when(dataLakeDao.isNewPdcExposure(isA(String.class), isA(String.class))).thenReturn(true);
        doNothing().when(dataLakeDao).storeDataLakes(anyList());
    }
}