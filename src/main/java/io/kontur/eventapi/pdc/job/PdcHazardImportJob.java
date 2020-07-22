package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.bucket4j.Bucket;
import io.kontur.eventapi.dao.HazardDataDao;
import io.kontur.eventapi.dto.HazardData;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class PdcHazardImportJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(PdcHazardImportJob.class);

    private final HpSrvClient hpSrvClient;
    private final Bucket bucket;
    private final HazardDataDao hazardDataDao;

    @Autowired
    public PdcHazardImportJob(HpSrvClient hpSrvClient, Bucket bucket,
                              HazardDataDao hazardDataDao) {
        this.hpSrvClient = hpSrvClient;
        this.bucket = bucket;
        this.hazardDataDao = hazardDataDao;
    }

    @Override
    public void run() {
        LOG.info("Run Forest, Run!");
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("createDate", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);

        JsonNode pdcHazardDtos = obtainHazardsBatch(searchBody);

        while (!pdcHazardDtos.isEmpty()) {
            pdcHazardDtos.forEach(node -> hazardDataDao.storeHazardData(convertHazardData((ObjectNode) node)));

            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + pdcHazardDtos.size());
            pdcHazardDtos = obtainHazardsBatch(searchBody);
        }
    }

    private JsonNode obtainHazardsBatch(HpSrvSearchBody searchBody) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.searchForHazards(searchBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private HazardData convertHazardData(ObjectNode node) {
        HazardData hazardData = new HazardData();
        hazardData.setHazardId(node.get("hazard_ID").asText());
        LocalDateTime createDate = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(node.get("create_Date").asLong()), ZoneOffset.UTC);
        hazardData.setCreateDate(createDate);
        hazardData.setProvider("hpSrv");
        hazardData.setUploadDate(LocalDateTime.now(ZoneOffset.UTC));
        hazardData.setData(node.toString());
        return hazardData;
    }
}
