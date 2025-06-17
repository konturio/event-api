package io.kontur.eventapi.pdc.service;

import com.fasterxml.jackson.databind.JsonNode;
import feign.RetryableException;
import io.github.bucket4j.Bucket;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HpSrvService {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvService.class);

    private final DataLakeDao dataLakeDao;
    private final Bucket bucket;
    private final HpSrvClient hpSrvClient;
    private final PdcDataLakeConverter pdcDataLakeConverter;

    public HpSrvService(DataLakeDao dataLakeDao, Bucket bucket, HpSrvClient hpSrvClient,
                        PdcDataLakeConverter pdcDataLakeConverter) {
        this.dataLakeDao = dataLakeDao;
        this.bucket = bucket;
        this.hpSrvClient = hpSrvClient;
        this.pdcDataLakeConverter = pdcDataLakeConverter;
    }

    public JsonNode obtainHazards(HpSrvSearchBody searchBody) {
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

    public JsonNode obtainMagsFeatureCollection(String hazardId) {
        try {
            return obtainMagsScheduled(hazardId);
        } catch (RetryableException e) {
            LOG.warn(e.getMessage());
            //will try once again
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(e);
            }
            return obtainMagsScheduled(hazardId);
        }
    }

    public JsonNode obtainProducts(HpSrvSearchBody searchBody) {
        try {
            return obtainProductsScheduled(searchBody);
        } catch (RetryableException e) {
            LOG.warn(e.getMessage());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(e);
            }
            return obtainProductsScheduled(searchBody);
        }
    }

    public void saveHazard(JsonNode node) {
        DataLake dataLake = pdcDataLakeConverter.convertHpSrvHazardData(node);
        dataLakeDao.storeEventData(dataLake);
    }

    public void saveMag(String externalId, JsonNode json) {
        if (json != null && !json.isEmpty() && !json.get("features").isEmpty()) {
            pdcDataLakeConverter.convertHpSrvMagData(json, externalId)
                    .forEach(dataLakeDao::storeEventData);
        }
    }

    public void saveProduct(JsonNode node) {
        DataLake dataLake = pdcDataLakeConverter.convertHpSrvProductData(node);
        if (dataLake.getUpdatedAt() == null || Boolean.TRUE.equals(dataLakeDao.isNewEvent(
                dataLake.getExternalId(), PdcDataLakeConverter.HP_SRV_PRODUCT_PROVIDER,
                dataLake.getUpdatedAt().toInstant().toString()))) {
            dataLakeDao.storeEventData(dataLake);
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

    private JsonNode obtainMagsScheduled(String hazardId) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.getMags(hazardId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode obtainProductsScheduled(HpSrvSearchBody searchBody) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.searchProducts(searchBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
