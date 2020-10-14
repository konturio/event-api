package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.service.HpSrvService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HpSrvMagsJobTest {

    @Mock
    HpSrvService hpSrvService;
    @Mock
    DataLakeDao dataLakeDao;
    @Captor
    ArgumentCaptor<String> hazardIdCaptor;
    @Captor
    ArgumentCaptor<String> externalIdCaptor;

    @Test
    public void testMagsImport() {
        //given
        ObjectNode node = new ObjectMapper().createObjectNode();
        when(hpSrvService.obtainMagsFeatureCollection(anyString())).thenReturn(node);

        var dataLakeList = List.of(generateHazardDataLake("1"), generateHazardDataLake("2"));
        when(dataLakeDao.getPdcHpSrvHazardsWithoutAreas()).thenReturn(dataLakeList);

        //when
        HpSrvMagsJob hpSrvMagsJob = new HpSrvMagsJob(dataLakeDao, hpSrvService);
        hpSrvMagsJob.run();

        //then
        verify(hpSrvService, times(2)).obtainMagsFeatureCollection(hazardIdCaptor.capture());
        verify(hpSrvService, times(2)).saveMag(externalIdCaptor.capture(), eq(node));

        List<String> hazardIdValues = hazardIdCaptor.getAllValues();
        assertEquals(Arrays.asList("hazardId1", "hazardId2"), hazardIdValues);

        List<String> externalIdValues = externalIdCaptor.getAllValues();
        assertEquals(Arrays.asList("extID1", "extID2"), externalIdValues);
    }

    @Test
    public void testNoHazardsToProcess() {
        //given
        HpSrvService hpSrvService = mock(HpSrvService.class);

        DataLakeDao dataLakeDao = mock(DataLakeDao.class);
        when(dataLakeDao.getPdcHpSrvHazardsWithoutAreas()).thenReturn(Collections.emptyList());

        //when
        HpSrvMagsJob hpSrvMagsJob = new HpSrvMagsJob(dataLakeDao, hpSrvService);
        hpSrvMagsJob.run();

        //then
        verify(hpSrvService, never()).obtainMagsFeatureCollection(any());
        verify(hpSrvService, never()).saveMag(any(), any());
    }

    private DataLake generateHazardDataLake(String id) {
        DataLake dataLake = new DataLake();
        dataLake.setExternalId("extID" + id);
        dataLake.setProvider(HP_SRV_SEARCH_PROVIDER);
        dataLake.setData(String.format( "{\"hazard_ID\": \"hazardId%s\"}", id));
        OffsetDateTime now = OffsetDateTime.now();
        dataLake.setLoadedAt(now);
        dataLake.setUpdatedAt(now);
        return dataLake;
    }

}