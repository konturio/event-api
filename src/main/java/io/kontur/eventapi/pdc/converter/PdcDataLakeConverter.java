package io.kontur.eventapi.pdc.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kontur.eventapi.entity.DataLake;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PdcDataLakeConverter {

    public final static String HP_SRV_SEARCH_PROVIDER = "hpSrvSearch";
    public final static String HP_SRV_MAG_PROVIDER = "hpSrvMag";
    public final static String PDC_SQS_PROVIDER = "pdcSqs";
    public final static DateTimeFormatter magsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static DataLake convertHpSrvHazardData(ObjectNode node) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(node.get("uuid").asText());
        dataLake.setUpdatedAt(getDateTimeFromMillis(node.get("update_Date")));
        dataLake.setProvider(HP_SRV_SEARCH_PROVIDER);
        dataLake.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));
        dataLake.setData(node.toString());
        return dataLake;
    }

    public static DataLake convertHpSrvMagData(JsonNode jsonNode, String eventId) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(eventId);
        dataLake.setProvider(HP_SRV_MAG_PROVIDER);
        dataLake.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));

        dataLake.setData(jsonNode.toString());
        return dataLake;
    }

    public static DataLake convertSQSMessage(String messageJson, String type, String messageId) {
        if (!"HAZARD".equals(type) && !"MAG".equals(type)) {
            throw new IllegalStateException("Unexpected SQS message type: " + type);
        }

        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setProvider(PDC_SQS_PROVIDER);
        dataLake.setLoadedAt(OffsetDateTime.now(ZoneOffset.UTC));
        dataLake.setData(messageJson);
        dataLake.setExternalId(messageId);

        return dataLake;
    }

    private static OffsetDateTime getDateTimeFromMillis(JsonNode node) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(node.asLong()), ZoneOffset.UTC);
    }
}
