package io.kontur.eventapi.pdc.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kontur.eventapi.dto.EventDataLakeDto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static io.kontur.eventapi.pdc.job.HpSrvSearchJob.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.job.HpSrvSearchJob.HP_SRV_SEARCH_PROVIDER;

public class PdcEventDataLakeConverter {

    public final static DateTimeFormatter magsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static EventDataLakeDto convertHazardData(ObjectNode node) {
        EventDataLakeDto eventDataLakeDto = new EventDataLakeDto();
        eventDataLakeDto.setObservationId(UUID.randomUUID());
        eventDataLakeDto.setExternalId(node.get("hazard_ID").asText());
        eventDataLakeDto.setUpdatedAt(getDateTimeFromNode(node.get("update_Date")));
        eventDataLakeDto.setProvider(HP_SRV_SEARCH_PROVIDER);
        eventDataLakeDto.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));
        eventDataLakeDto.setData(node.toString());
        return eventDataLakeDto;
    }

    public static EventDataLakeDto convertMagData(JsonNode jsonNode, String eventId) {
        EventDataLakeDto eventDataLakeDto = new EventDataLakeDto();
        eventDataLakeDto.setObservationId(UUID.randomUUID());
        eventDataLakeDto.setExternalId(eventId);
        eventDataLakeDto.setProvider(HP_SRV_MAG_PROVIDER);
        eventDataLakeDto.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));
        eventDataLakeDto.setData(jsonNode.toString());
        return eventDataLakeDto;
    }

    private static OffsetDateTime getDateTimeFromNode(JsonNode node) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(node.asLong()), ZoneOffset.UTC);
    }

}
