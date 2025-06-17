package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import io.kontur.eventapi.pdc.service.HpSrvService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_PRODUCT_PROVIDER;

@Component
public class HpSrvProductsJob extends AbstractJob {

    private final DataLakeDao dataLakeDao;
    private final HpSrvService hpSrvService;

    @Autowired
    public HpSrvProductsJob(DataLakeDao dataLakeDao, HpSrvService hpSrvService, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.dataLakeDao = dataLakeDao;
        this.hpSrvService = hpSrvService;
    }

    @Override
    public void execute() {
        importProducts();
    }

    @Override
    public String getName() {
        return "hpSrvProductImport";
    }

    private void importProducts() {
        HpSrvSearchBody searchBody = generateProductSearchBody();
        JsonNode products = hpSrvService.obtainProducts(searchBody);

        while (!products.isEmpty()) {
            products.forEach(hpSrvService::saveProduct);
            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + products.size());
            products = hpSrvService.obtainProducts(searchBody);
        }
    }

    private HpSrvSearchBody generateProductSearchBody() {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getOrder().getOrderList().put("productId", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);

        dataLakeDao.getLatestUpdatedHazard(HP_SRV_PRODUCT_PROVIDER)
                .map(DataLake::getUpdatedAt)
                .ifPresent(lastUpdateTime -> searchBody.addAndRestriction("GREATER_THAN", "updateDate",
                        convertOffsetDateTimeToEpochMillis(lastUpdateTime)));
        return searchBody;
    }

    private String convertOffsetDateTimeToEpochMillis(OffsetDateTime dateTime) {
        return String.valueOf(dateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }
}
