package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import io.kontur.eventapi.pdc.service.HpSrvService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_PRODUCT_PROVIDER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HpSrvProductsJobTest {

    @Mock
    HpSrvService hpSrvService;
    @Mock
    DataLakeDao dataLakeDao;

    @Test
    public void testPagination() {
        HpSrvSearchBody body0 = generateSearchBody(0);
        when(hpSrvService.obtainProducts(eq(body0))).thenReturn(generateArrayNodeSize20());

        HpSrvSearchBody body20 = generateSearchBody(20);
        when(hpSrvService.obtainProducts(eq(body20))).thenReturn(generateArrayNodeSize10());

        HpSrvSearchBody body30 = generateSearchBody(30);
        when(hpSrvService.obtainProducts(eq(body30))).thenReturn(generateArrayNodeSize0());

        HpSrvProductsJob job = new HpSrvProductsJob(dataLakeDao, hpSrvService, new SimpleMeterRegistry());
        job.run();

        verify(hpSrvService, times(30)).saveProduct(any());
        verify(hpSrvService, times(3)).obtainProducts(any(HpSrvSearchBody.class));
    }

    @Test
    public void testRestartFromLatest() {
        long testMillis = 1602445516323L;
        Instant instant = Instant.ofEpochMilli(testMillis);
        OffsetDateTime now = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());

        when(hpSrvService.obtainProducts(any(HpSrvSearchBody.class))).thenReturn(generateArrayNodeSize0());
        DataLake dataLake = mock(DataLake.class);
        when(dataLake.getUpdatedAt()).thenReturn(now);
        when(dataLakeDao.getLatestUpdatedHazard(HP_SRV_PRODUCT_PROVIDER)).thenReturn(Optional.of(dataLake));

        HpSrvProductsJob job = new HpSrvProductsJob(dataLakeDao, hpSrvService, new SimpleMeterRegistry());
        job.run();

        verify(dataLakeDao).getLatestUpdatedHazard(HP_SRV_PRODUCT_PROVIDER);
        HpSrvSearchBody body = generateSearchBody(0);
        body.addAndRestriction("GREATER_THAN", "updateDate", String.valueOf(testMillis));
        verify(hpSrvService).obtainProducts(eq(body));
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

    private HpSrvSearchBody generateSearchBody(int offset) {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getOrder().getOrderList().put("productId", "ASC");
        searchBody.getPagination().setOffset(offset);
        searchBody.getPagination().setPageSize(20);
        return searchBody;
    }
}
