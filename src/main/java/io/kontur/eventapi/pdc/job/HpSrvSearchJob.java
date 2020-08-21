package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.RetryableException;
import io.github.bucket4j.Bucket;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class HpSrvSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvSearchJob.class);
    public final static String HP_SRV_SEARCH_PROVIDER = "hpSrvSearch";
    public final static String HP_SRV_MAG_PROVIDER = "hpSrvMag";

    private final HpSrvClient hpSrvClient;
    private final Bucket bucket;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public HpSrvSearchJob(HpSrvClient hpSrvClient, Bucket bucket,
                          DataLakeDao dataLakeDao) {
        this.hpSrvClient = hpSrvClient;
        this.bucket = bucket;
        this.dataLakeDao = dataLakeDao;
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
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);

        dataLakeDao.getLatestUpdatedHazard(HP_SRV_SEARCH_PROVIDER)
                .map(DataLake::getUpdatedAt)
                .ifPresent(lastUpdateTime -> searchBody.addAndRestriction("GREATER_THAN", "updateDate",
                        convertOffsetDateTimeToEpochMillis(lastUpdateTime)));

        JsonNode pdcHazardDtos = obtainHazards(searchBody);

        while (!pdcHazardDtos.isEmpty()) {
            pdcHazardDtos.forEach(node -> dataLakeDao
                    .storeEventData(PdcDataLakeConverter.convertHazardData((ObjectNode) node)));

            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + pdcHazardDtos.size());
            pdcHazardDtos = obtainHazards(searchBody);
        }
    }

    private JsonNode obtainHazards(HpSrvSearchBody searchBody) {
        try {
            return obtainHazardsInSchedule(searchBody);
        } catch (RetryableException e) {
            LOG.warn(e.getMessage());
            //will try once again
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(e);
            }
            return obtainHazardsInSchedule(searchBody);
        }
    }

    private JsonNode obtainHazardsInSchedule(HpSrvSearchBody searchBody) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.searchHazards(searchBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void importMags() {
        List<String> eventsWithoutAreas = dataLakeDao.getPdcEventsWithoutAreas();
        LOG.info("{} hazards to process", eventsWithoutAreas.size());

        for (int i = 0; i < eventsWithoutAreas.size(); i++) {
            if ((eventsWithoutAreas.size() - i) % 100 == 0) {
                LOG.info("{} hazards to process", eventsWithoutAreas.size() - i);
            }

            String eventId = eventsWithoutAreas.get(i);
            try {
                JsonNode json = obtainMagsFeatureCollection(eventId);
                if (!json.isEmpty() && !json.get("features").isEmpty()) {
                    DataLake magDto = PdcDataLakeConverter.convertMagData(json, eventId);
                    dataLakeDao.storeEventData(magDto);
                }
            } catch (Exception e) {
                LOG.warn("Exception during hazard mag processing. Hazard Id = '{}'", eventId, e);
            }
        }
    }

    private JsonNode obtainMagsFeatureCollection(String eventId) {
        try {
            return obtainMagsInSchedule(eventId);
        } catch (RetryableException e) {
            LOG.warn(e.getMessage());
            //will try once again
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(e);
            }
            return obtainMagsInSchedule(eventId);
        }
    }

    private JsonNode obtainMagsInSchedule(String eventId) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.getMags(eventId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertOffsetDateTimeToEpochMillis(OffsetDateTime dateTime) {
        return String.valueOf(dateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }
}
