package io.kontur.eventapi.pdc.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.RetryableException;
import io.github.bucket4j.Bucket;
import io.kontur.eventapi.dao.EventDataLakeDao;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.pdc.client.HpSrvClient;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class HpSrvSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(HpSrvSearchJob.class);
    public final static String HP_SRV_SEARCH_PROVIDER = "hpSrvSearch";
    public final static String HP_SRV_MAG_PROVIDER = "hpSrvMag";

    private final DateTimeFormatter magsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
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
        LOG.info("PDC hazards import job has started");
        OffsetDateTime lastUpdateTime = eventDataLakeDao.getLatestUpdatedHazard(HP_SRV_SEARCH_PROVIDER)
                .map(EventDataLakeDto::getUpdateDate)
                .orElse(null);

        LOG.info("PDC hazards import has started");
        importHazards(lastUpdateTime);
        LOG.info("PDC hazards import has finished");

        LOG.info("PDC mags import has started");
        importMags();
        LOG.info("PDC mags import has finished");

        LOG.info("PDC hazards import job has finished");
    }

    private void importHazards(OffsetDateTime lastUpdateTime) {
        HpSrvSearchBody searchBody = new HpSrvSearchBody();
        searchBody.getOrder().getOrderList().put("updateDate", "ASC");
        searchBody.getPagination().setOffset(0);
        searchBody.getPagination().setPageSize(20);

        if (lastUpdateTime != null) {
            searchBody.addAndRestriction("GREATER_THAN", "updateDate",
                    convertOffsetDateTimeToEpochMillis(lastUpdateTime));
        }

        JsonNode pdcHazardDtos = obtainHazards(searchBody);

        while (!pdcHazardDtos.isEmpty()) {
            pdcHazardDtos.forEach(node -> eventDataLakeDao.storeEventData(convertHazardData((ObjectNode) node)));

            searchBody.getPagination().setOffset(searchBody.getPagination().getOffset() + pdcHazardDtos.size());
            pdcHazardDtos = obtainHazards(searchBody);
        }
    }

    private void importMags() {
        List<String> eventsWithoutAreas = eventDataLakeDao.getPdcEventsWithoutAreas();
        LOG.info("{} hazards to process", eventsWithoutAreas.size());

        for (int i = 0; i < eventsWithoutAreas.size(); i++) {
            if ((eventsWithoutAreas.size() - i) % 100 == 0) {
                LOG.info("{} hazards to process", eventsWithoutAreas.size() - i);
            }

            String eventId = eventsWithoutAreas.get(i);
            List<Feature> mags = obtainAndSortMags(eventId);
            for (Feature mag : mags) {
                EventDataLakeDto magDto = convertMagData(mag);
                eventDataLakeDao.storeEventData(magDto);
            }
        }
    }

    private List<Feature> obtainAndSortMags(String eventId) {
        FeatureCollection fc = obtainMags(eventId);
        if (fc == null) {
            LOG.info("No mags were found for Hazard with id: {}", eventId);
            return Collections.emptyList();
        }
        List<Feature> features = Arrays.asList(fc.getFeatures());

        features.sort(Comparator.comparing(feature -> OffsetDateTime
                .parse(feature.getProperties().get("updateDate").toString(), magsDateTimeFormatter)));
        return features;
    }

    private FeatureCollection obtainMags(String eventId) {
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
            return obtainMags(eventId);
        }
    }

    private FeatureCollection obtainMagsInSchedule(String eventId) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.getMags(eventId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
            return obtainHazards(searchBody);
        }
    }

    private JsonNode obtainHazardsInSchedule(HpSrvSearchBody searchBody) {
        try {
            bucket.asScheduler().consume(1);
            return hpSrvClient.getAuthorizationTokens(searchBody);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private EventDataLakeDto convertHazardData(ObjectNode node) {
        EventDataLakeDto eventDataLakeDto = new EventDataLakeDto();
        eventDataLakeDto.setHazardId(node.get("hazard_ID").asText());
        eventDataLakeDto.setCreateDate(getDateTimeFromNode(node, "create_Date"));
        eventDataLakeDto.setUpdateDate(getDateTimeFromNode(node, "update_Date"));
        eventDataLakeDto.setProvider(HP_SRV_SEARCH_PROVIDER);
        eventDataLakeDto.setUploadDate(OffsetDateTime.now(ZoneOffset.UTC));
        eventDataLakeDto.setData(node.toString());
        return eventDataLakeDto;
    }

    private EventDataLakeDto convertMagData(Feature feature) {
        EventDataLakeDto eventDataLakeDto = new EventDataLakeDto();
        eventDataLakeDto.setHazardId(String.valueOf(feature.getProperties().get("hazard.hazardId")));
        eventDataLakeDto.setCreateDate(OffsetDateTime
                .parse(feature.getProperties().get("createDate").toString(), magsDateTimeFormatter));
        eventDataLakeDto.setUpdateDate(OffsetDateTime
                .parse(feature.getProperties().get("updateDate").toString(), magsDateTimeFormatter));
        eventDataLakeDto.setProvider(HP_SRV_MAG_PROVIDER);
        eventDataLakeDto.setUploadDate(OffsetDateTime.now(ZoneOffset.UTC));
        eventDataLakeDto.setData(feature.toString());
        return eventDataLakeDto;
    }

    private String convertOffsetDateTimeToEpochMillis(OffsetDateTime dateTime) {
        return String.valueOf(dateTime.atZoneSameInstant(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    private OffsetDateTime getDateTimeFromNode(ObjectNode node, String fieldName) {
        return OffsetDateTime
                .ofInstant(Instant.ofEpochMilli(node.get(fieldName).asLong()), ZoneOffset.UTC);
    }
}
