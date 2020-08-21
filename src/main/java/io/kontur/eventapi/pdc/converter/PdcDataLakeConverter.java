package io.kontur.eventapi.pdc.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kontur.eventapi.entity.DataLake;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static io.kontur.eventapi.pdc.job.HpSrvSearchJob.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.job.HpSrvSearchJob.HP_SRV_SEARCH_PROVIDER;

public class PdcDataLakeConverter {

    public final static DateTimeFormatter magsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static DataLake convertHazardData(ObjectNode node) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(node.get("hazard_ID").asText());
        dataLake.setUpdatedAt(getDateTimeFromMillis(node.get("update_Date")));
        dataLake.setProvider(HP_SRV_SEARCH_PROVIDER);
        dataLake.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));
        dataLake.setData(node.toString());
        return dataLake;
    }

    public static DataLake convertMagData(JsonNode jsonNode, String eventId) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(eventId);
        dataLake.setProvider(HP_SRV_MAG_PROVIDER);
        dataLake.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));

        dataLake.setData(jsonNode.toString());
        return dataLake;
    }

    private static OffsetDateTime getDateTimeFromMillis(JsonNode node) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(node.asLong()), ZoneOffset.UTC);
    }
}
