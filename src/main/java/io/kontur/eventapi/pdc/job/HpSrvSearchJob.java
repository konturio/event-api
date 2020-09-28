package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import feign.RetryableException;
import io.github.bucket4j.Bucket;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import io.kontur.eventapi.pdc.service.HpSrvService;
import io.kontur.eventapi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;

@Component
public class HpSrvSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvSearchJob.class);

    private final HpSrvClient hpSrvClient;
    private final Bucket bucket;
    private final DataLakeDao dataLakeDao;
    private final HpSrvService hpSrvService;

    @Autowired
    public HpSrvSearchJob(HpSrvClient hpSrvClient, Bucket bucket,
                          DataLakeDao dataLakeDao, HpSrvService hpSrvService) {
        this.hpSrvClient = hpSrvClient;
        this.bucket = bucket;
        this.dataLakeDao = dataLakeDao;
        this.hpSrvService = hpSrvService;
    }

    @Override
    public void run() {
        LOG.info("PDC hazards import job has started");

        LOG.info("PDC hazards import has started");
        importHazards();
        LOG.info("PDC hazards import has finished");

        LOG.info("PDC mags import has started");
        importMags();
        LOG.info("PDC mags import has finished");

        LOG.info("PDC hazards import job has finished");
    }

    private void importHazards() {
        HpSrvSearchBody searchBody = generateHazardSearchBody();

        JsonNode pdcHazardDtos = obtainHazards(searchBody);

        while (!pdcHazardDtos.isEmpty()) {
            pdcHazardDtos.forEach(hpSrvService::saveHazard);

            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + pdcHazardDtos.size());
            pdcHazardDtos = obtainHazards(searchBody);
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

    private JsonNode obtainHazards(HpSrvSearchBody searchBody) {
        try {
            return obtainHazardsScheduled(searchBody);
        } catch (RetryableException e) {
            LOG.warn(e.getMessage());
            //will try once again
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(e);
            }
            return obtainHazardsScheduled(searchBody);
        }
    }

    private JsonNode obtainHazardsScheduled(HpSrvSearchBody searchBody) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.searchHazards(searchBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void importMags() {
        List<DataLake> eventsWithoutAreas = dataLakeDao.getPdcEventsWithoutAreas();
        LOG.info("{} hazards to process", eventsWithoutAreas.size());

        for (int i = 0; i < eventsWithoutAreas.size(); i++) {
            if ((eventsWithoutAreas.size() - i) % 100 == 0) {
                LOG.info("{} hazards to process", eventsWithoutAreas.size() - i);
            }

            DataLake dataLake = eventsWithoutAreas.get(i);
            String externalId = dataLake.getExternalId();
            try {
                JsonNode json = obtainMagsFeatureCollection(dataLake);
                hpSrvService.saveMag(externalId, json);
            } catch (Exception e) {
                LOG.warn("Exception during hazard mag processing. Hazard UUID = '{}'", externalId, e);
            }
        }
    }

    private JsonNode obtainMagsFeatureCollection(DataLake dataLake) {
        try {
            return obtainMagsScheduled(dataLake);
        } catch (RetryableException e) {
            LOG.warn(e.getMessage());
            //will try once again
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(e);
            }
            return obtainMagsScheduled(dataLake);
        }
    }

    private JsonNode obtainMagsScheduled(DataLake dataLake) {
        try {
            String hazardId = getHazardId(dataLake);
            bucket.asScheduler().consume(1);
            return hpSrvClient.getMags(hazardId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHazardId(DataLake dataLake) {
        JsonNode jsonNode = JsonUtil.readTree(dataLake.getData());
        return jsonNode.get("hazard_ID").asText();
    }

    private String convertOffsetDateTimeToEpochMillis(OffsetDateTime dateTime) {
        return String.valueOf(dateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }
}
