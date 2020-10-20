package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import io.kontur.eventapi.pdc.service.HpSrvService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;

@Component
public class HpSrvSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvSearchJob.class);

    private final DataLakeDao dataLakeDao;
    private final HpSrvService hpSrvService;

    @Autowired
    public HpSrvSearchJob(DataLakeDao dataLakeDao, HpSrvService hpSrvService) {
        this.dataLakeDao = dataLakeDao;
        this.hpSrvService = hpSrvService;
    }

    @Override
    @Timed(value = "job.pdc.hpSrvSearch", longTask = true)
    public void run() {
        LOG.info("PDC hazards import job has started");
        importHazards();
        LOG.info("PDC hazards import job has finished");
    }

    private void importHazards() {
        HpSrvSearchBody searchBody = generateHazardSearchBody();

        JsonNode pdcHazardDtos = hpSrvService.obtainHazards(searchBody);

        while (!pdcHazardDtos.isEmpty()) {
            pdcHazardDtos.forEach(hpSrvService::saveHazard);

            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + pdcHazardDtos.size());
            pdcHazardDtos = hpSrvService.obtainHazards(searchBody);
        }
    }

    private HpSrvSearchBody generateHazardSearchBody() {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getOrder().getOrderList().put("hazardId", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);

        dataLakeDao.getLatestUpdatedHazard(HP_SRV_SEARCH_PROVIDER)
                .map(DataLake::getUpdatedAt)
                .ifPresent(lastUpdateTime -> searchBody.addAndRestriction("GREATER_THAN", "updateDate",
                        convertOffsetDateTimeToEpochMillis(lastUpdateTime)));
        return searchBody;
    }

    private String convertOffsetDateTimeToEpochMillis(OffsetDateTime dateTime) {
        return String.valueOf(dateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }
}
