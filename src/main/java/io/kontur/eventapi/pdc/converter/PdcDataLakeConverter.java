package io.kontur.eventapi.pdc.converter;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.util.JsonUtil;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PdcDataLakeConverter {

    public final static String HP_SRV_SEARCH_PROVIDER = "hpSrvSearch";
    public final static String HP_SRV_MAG_PROVIDER = "hpSrvMag";
    public final static String PDC_SQS_PROVIDER = "pdcSqs";
    public final static DateTimeFormatter magsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public DataLake convertHpSrvHazardData(JsonNode node) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(node.get("uuid").asText());
        dataLake.setUpdatedAt(getDateTimeFromMillis(node.get("update_Date")));
        dataLake.setProvider(HP_SRV_SEARCH_PROVIDER);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(node.toString());
        return dataLake;
    }

    public List<DataLake> convertHpSrvMagData(JsonNode jsonNode, String eventId) {
        FeatureCollection fc = JsonUtil.readJson(jsonNode.toString(), FeatureCollection.class);
        List<DataLake> result = new ArrayList<>(fc.getFeatures().length);

        for (Feature feature : fc.getFeatures()) {
            DataLake dataLake = new DataLake();
            dataLake.setObservationId(UUID.randomUUID());
            dataLake.setExternalId(eventId);
            dataLake.setProvider(HP_SRV_MAG_PROVIDER);
            dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
            dataLake.setData(JsonUtil.writeJson(new FeatureCollection(new Feature[]{feature})));
            result.add(dataLake);
        }

        return result;
    }

    public DataLake convertSQSMessage(String messageJson, String type, String messageId) {
        if (!"HAZARD".equals(type) && !"MAG".equals(type)) {
            throw new IllegalStateException("Unexpected SQS message type: " + type);
        }

        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setProvider(PDC_SQS_PROVIDER);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(messageJson);
        dataLake.setExternalId(messageId);

        return dataLake;
    }

    private OffsetDateTime getDateTimeFromMillis(JsonNode node) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(node.asLong()), ZoneOffset.UTC);
    }
}
