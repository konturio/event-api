package io.kontur.eventapi.pdc.converter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kontur.eventapi.dto.EventDataLakeDto;
import org.wololo.geojson.Feature;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static io.kontur.eventapi.pdc.job.HpSrvSearchJob.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.job.HpSrvSearchJob.HP_SRV_SEARCH_PROVIDER;

public class PdcEventDataLakeConverter {

    private final static DateTimeFormatter magsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    public static EventDataLakeDto convertHazardData(ObjectNode node) {
        EventDataLakeDto eventDataLakeDto = new EventDataLakeDto();
        eventDataLakeDto.setHazardId(node.get("hazard_ID").asText());
        eventDataLakeDto.setCreateDate(getDateTimeFromNode(node, "create_Date"));
        eventDataLakeDto.setUpdateDate(getDateTimeFromNode(node, "update_Date"));
        eventDataLakeDto.setProvider(HP_SRV_SEARCH_PROVIDER);
        eventDataLakeDto.setUploadDate(OffsetDateTime.now(ZoneOffset.UTC));
        eventDataLakeDto.setData(node.toString());
        return eventDataLakeDto;
    }

    public static EventDataLakeDto convertMagData(Feature feature) {
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

    private static OffsetDateTime getDateTimeFromNode(ObjectNode node, String fieldName) {
        return OffsetDateTime
                .ofInstant(Instant.ofEpochMilli(node.get(fieldName).asLong()), ZoneOffset.UTC);
    }

}
