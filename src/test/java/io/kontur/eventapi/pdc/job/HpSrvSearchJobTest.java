package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import io.kontur.eventapi.pdc.service.HpSrvService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HpSrvSearchJobTest {

    @Mock
    HpSrvService hpSrvService;
    @Mock
    DataLakeDao dataLakeDao;

    @Test
    public void testPagination() {
        //given
        HpSrvSearchBody offset0SearchBody = generateHazardSearchBodyOffset0();
        when(hpSrvService.obtainHazards(eq(offset0SearchBody))).thenReturn(generateArrayNodeSize20());

        HpSrvSearchBody offset20SearchBody = generateHazardSearchBodyOffset20();
        when(hpSrvService.obtainHazards(eq(offset20SearchBody))).thenReturn(generateArrayNodeSize10());

        HpSrvSearchBody offset30SearchBody = generateHazardSearchBodyOffset30();
        when(hpSrvService.obtainHazards(eq(offset30SearchBody))).thenReturn(generateArrayNodeSize0());

        //when
        HpSrvSearchJob hpSrvSearchJob = new HpSrvSearchJob(dataLakeDao, hpSrvService);
        hpSrvSearchJob.run();

        //then
        verify(hpSrvService, times(30)).saveHazard(any());
        verify(hpSrvService, times(3)).obtainHazards(any(HpSrvSearchBody.class));
    }

    @Test
    public void testJobContinuesFromLatestHazard() {
        //given
        long testMillis = 1602445516323L;
        Instant instant = Instant.ofEpochMilli(testMillis);
        OffsetDateTime now = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());

        when(hpSrvService.obtainHazards(any(HpSrvSearchBody.class))).thenReturn(generateArrayNodeSize0());

        DataLake dataLake = mock(DataLake.class);
        when(dataLake.getUpdatedAt()).thenReturn(now);
        when(dataLakeDao.getLatestUpdatedHazard(HP_SRV_SEARCH_PROVIDER)).thenReturn(Optional.of(dataLake));

        //when
        HpSrvSearchJob hpSrvSearchJob = new HpSrvSearchJob(dataLakeDao, hpSrvService);
        hpSrvSearchJob.run();

        //then
        verify(dataLakeDao, times(1)).getLatestUpdatedHazard(HP_SRV_SEARCH_PROVIDER);
        HpSrvSearchBody offset0SearchBody = generateHazardSearchBodyOffset0();
        offset0SearchBody.addAndRestriction("GREATER_THAN", "updateDate", String.valueOf(testMillis));
        verify(hpSrvService, times(1)).obtainHazards(eq(offset0SearchBody));
    }

    private ArrayNode generateArrayNodeSize20() {
        ArrayNode arrayNodeSize20 = new ObjectMapper().createArrayNode();
        for (int i = 0; i < 20; i++) {
            arrayNodeSize20.add(i);
        }
        return arrayNodeSize20;
    }

    private ArrayNode generateArrayNodeSize10() {
        ArrayNode arrayNodeSize20 = new ObjectMapper().createArrayNode();
        for (int i = 0; i < 10; i++) {
            arrayNodeSize20.add(i);
        }
        return arrayNodeSize20;
    }

    private ArrayNode generateArrayNodeSize0() {
        return new ObjectMapper().createArrayNode();
    }

    private HpSrvSearchBody generateHazardSearchBodyOffset0() {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getOrder().getOrderList().put("hazardId", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);
        return searchBody;
    }

    private HpSrvSearchBody generateHazardSearchBodyOffset20() {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getOrder().getOrderList().put("hazardId", "ASC");
        searchBody.getPagination().setOffset(20);
        searchBody.getPagination().setPageSize(20);

        return searchBody;
    }

    private HpSrvSearchBody generateHazardSearchBodyOffset30() {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getOrder().getOrderList().put("hazardId", "ASC");
        searchBody.getPagination().setOffset(30);
        searchBody.getPagination().setPageSize(20);

        return searchBody;
    }
}