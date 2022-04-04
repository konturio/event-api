package io.kontur.eventapi.pdc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HpSrvServiceTest {

    @Mock
    DataLakeDao dataLakeDao;
    @Mock
    HpSrvClient hpSrvClient;
    @Mock
    PdcDataLakeConverter pdcDataLakeConverter;

    @Test
    public void testObtainHazards() {
        //given
        Bucket bucket = spy(getBucket());
        HpSrvService hpSrvService = new HpSrvService(dataLakeDao, bucket, hpSrvClient, pdcDataLakeConverter);
        HpSrvSearchBody searchBody = new HpSrvSearchBody();

        //when
        hpSrvService.obtainHazards(searchBody);

        //then
        verify(hpSrvClient, times(1)).searchHazards(searchBody);
        verify(bucket, times(1)).asScheduler();
    }

    @Test
    public void testObtainMagsFeatureCollection() {
        //given
        Bucket bucket = spy(getBucket());
        HpSrvService hpSrvService = new HpSrvService(dataLakeDao, bucket, hpSrvClient, pdcDataLakeConverter);

        //when
        hpSrvService.obtainMagsFeatureCollection("id1");

        //then
        verify(hpSrvClient, times(1)).getMags("id1");
        verify(bucket, times(1)).asScheduler();
    }

    public Bucket getBucket() {
        Refill refill = Refill.intervally(10, Duration.ofSeconds(1));
        Bandwidth limit = Bandwidth.classic(10, refill);
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    @Test
    void testSaveHazard() {
        //given
        HpSrvService hpSrvService = new HpSrvService(dataLakeDao, mock(Bucket.class), hpSrvClient, pdcDataLakeConverter);
        ObjectNode node = new ObjectMapper().createObjectNode();
        DataLake dataLake = new DataLake();
        when(pdcDataLakeConverter.convertHpSrvHazardData(node)).thenReturn(dataLake);

        //when
        hpSrvService.saveHazard(node);

        //then
        verify(pdcDataLakeConverter, times(1)).convertHpSrvHazardData(node);
        verify(dataLakeDao, times(1)).storeEventData(dataLake);
    }

    @Test
    void testSaveMag() {
        //given
        HpSrvService hpSrvService = new HpSrvService(dataLakeDao, mock(Bucket.class), hpSrvClient, pdcDataLakeConverter);
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.set("features", new ObjectMapper().createObjectNode().put("1", 1));
        DataLake dataLake = new DataLake();
        DataLake dataLake2 = new DataLake();
        when(pdcDataLakeConverter.convertHpSrvMagData(node, "exId1")).thenReturn(List.of(dataLake, dataLake2));

        //when
        hpSrvService.saveMag("exId1", node, null);

        //then
        verify(pdcDataLakeConverter, times(1)).convertHpSrvMagData(node, "exId1");
        verify(dataLakeDao, times(2)).storeEventData(any(DataLake.class));
    }

    @Test
    void testSaveEmptyMag() {
        //given
        HpSrvService hpSrvService = new HpSrvService(dataLakeDao, mock(Bucket.class), hpSrvClient, pdcDataLakeConverter);
        ObjectNode node = new ObjectMapper().createObjectNode();

        //when
        hpSrvService.saveMag("exId1", node, null);

        //then
        verify(pdcDataLakeConverter, never()).convertHpSrvMagData(any(), anyString());
        verify(dataLakeDao, never()).storeEventData(any());
    }
}