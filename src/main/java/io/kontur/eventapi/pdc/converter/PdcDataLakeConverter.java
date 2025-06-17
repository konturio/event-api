package io.kontur.eventapi.pdc.converter;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.util.JsonUtil;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.kontur.eventapi.entity.EventType.FLOOD;
import static io.kontur.eventapi.normalization.Normalizer.readString;
import static io.kontur.eventapi.pdc.normalization.PdcHazardNormalizer.ORIGIN_NASA;
import static io.kontur.eventapi.pdc.normalization.PdcHazardNormalizer.defineType;
import static io.kontur.eventapi.pdc.normalization.PdcSqsMessageNormalizer.*;
import static org.apache.commons.lang3.StringUtils.contains;

@Component
public class PdcDataLakeConverter {

    public final static String HP_SRV_SEARCH_PROVIDER = "hpSrvSearch";
    public final static String HP_SRV_MAG_PROVIDER = "hpSrvMag";
    public final static String HP_SRV_PRODUCT_PROVIDER = "hpSrvProduct";
    public final static String PDC_SQS_PROVIDER = "pdcSqs";
    public final static String PDC_MAP_SRV_PROVIDER = "pdcMapSrv";
    public final static String PDC_SQS_NASA_PROVIDER = "pdcSqsNasa";
    public final static String PDC_MAP_SRV_NASA_PROVIDER = "pdcMapSrvNasa";
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

    public DataLake convertHpSrvProductData(JsonNode node) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(node.get("uuid").asText());
        if (node.has("updateDate")) {
            dataLake.setUpdatedAt(getDateTimeFromMillis(node.get("updateDate")));
        }
        dataLake.setProvider(HP_SRV_PRODUCT_PROVIDER);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(node.toString());
        return dataLake;
    }

    public DataLake convertSQSMessage(String messageJson, String type, String messageId) {
        if (!"HAZARD".equals(type) && !"MAG".equals(type)) {
            throw new IllegalStateException("Unexpected SQS message type: " + type + "\n" + messageJson);
        }

        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setProvider(definePdcSqsProvider(messageJson));
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setData(messageJson);
        dataLake.setExternalId(messageId);

        return dataLake;
    }

    public DataLake convertExposure(String data, String externalId) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setProvider(definePdcMapSrvProvider(data));
        dataLake.setData(data);
        dataLake.setExternalId(externalId);

        OffsetDateTime now = DateTimeUtil.uniqueOffsetDateTime();
        dataLake.setLoadedAt(now);
        dataLake.setUpdatedAt(now);

        return dataLake;
    }

    private OffsetDateTime getDateTimeFromMillis(JsonNode node) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(node.asLong()), ZoneOffset.UTC);
    }

    private String definePdcSqsProvider(String data) {
        JsonNode event = parseEvent(data);
        Map<String, Object> props = parseProps(event);
        return "HAZARD".equals(getType(event))
                && FLOOD.equals(defineType(readString((Map<String, Object>) props.get("hazardType"), "typeId")))
                && contains(readString((Map<String, Object>) props.get("hazardDescription"), "description"), ORIGIN_NASA)
                || "MAG".equals(getType(event))
                && FLOOD.equals(defineType(readString((Map<String, Object>) ((Map<String, Object>) props.get("hazard")).get("hazardType"), "typeId")))
                && contains(readString((Map<String, Object>) ((Map<String, Object>) props.get("hazard")).get("hazardDescription"), "description"), ORIGIN_NASA)
                ? PDC_SQS_NASA_PROVIDER : PDC_SQS_PROVIDER;
    }

    private String definePdcMapSrvProvider(String data) {
        Feature feature = (Feature) GeoJSONFactory.create(data);
        return FLOOD.equals(defineType(readString(feature.getProperties(), "type_id")))
                && contains(readString(feature.getProperties(), "exp_description"), ORIGIN_NASA)
                ? PDC_MAP_SRV_NASA_PROVIDER : PDC_MAP_SRV_PROVIDER;
    }

}
