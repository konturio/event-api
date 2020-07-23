package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.bucket4j.Bucket;
import io.kontur.eventapi.dao.EventDataLakeDao;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class HpSrvSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvSearchJob.class);
    private final static String PROVIDER = "hpSrv";

    private final HpSrvClient hpSrvClient;
    private final Bucket bucket;
    private final EventDataLakeDao eventDataLakeDao;

    @Autowired
    public HpSrvSearchJob(HpSrvClient hpSrvClient, Bucket bucket,
                          EventDataLakeDao eventDataLakeDao) {
        this.hpSrvClient = hpSrvClient;
        this.bucket = bucket;
        this.eventDataLakeDao = eventDataLakeDao;
    }

    @Override
    public void run() {
        LOG.info("PdcHazardImportJob has started");
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);

        eventDataLakeDao.getLatestUpdatedHazard(PROVIDER).ifPresent(
                eventDataLakeDto -> searchBody
                        .addAndRestriction("GREATER_THAN", "updateDate", getUpdateDateEpochMillis(eventDataLakeDto)));

        JsonNode pdcHazardDtos = obtainHazardsBatch(searchBody);

        while (!pdcHazardDtos.isEmpty()) {
            pdcHazardDtos.forEach(node -> eventDataLakeDao.storeHazardData(convertHazardData((ObjectNode) node)));

            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + pdcHazardDtos.size());
            pdcHazardDtos = obtainHazardsBatch(searchBody);
        }
        LOG.info("PdcHazardImportJob has finished");
    }

    private String getUpdateDateEpochMillis(EventDataLakeDto eventDataLakeDto) {
        return String.valueOf(eventDataLakeDto.getUpdateDate().atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    private JsonNode obtainHazardsBatch(HpSrvSearchBody searchBody) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.searchForHazards(searchBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private EventDataLakeDto convertHazardData(ObjectNode node) {
        EventDataLakeDto eventDataLakeDto = new EventDataLakeDto();
        eventDataLakeDto.setHazardId(node.get("hazard_ID").asText());
        eventDataLakeDto.setCreateDate(getDateTimeFromNode(node, "create_Date"));
        eventDataLakeDto.setUpdateDate(getDateTimeFromNode(node, "update_Date"));
        eventDataLakeDto.setProvider(PROVIDER);
        eventDataLakeDto.setUploadDate(OffsetDateTime.now(ZoneOffset.UTC));
        eventDataLakeDto.setData(node.toString());
        return eventDataLakeDto;
    }

    private OffsetDateTime getDateTimeFromNode(ObjectNode node, String fieldName) {
        return OffsetDateTime
                .ofInstant(Instant.ofEpochMilli(node.get(fieldName).asLong()), ZoneOffset.UTC);
    }
}
