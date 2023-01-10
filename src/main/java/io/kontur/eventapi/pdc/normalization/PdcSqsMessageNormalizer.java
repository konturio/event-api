package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.entity.EventType.CYCLONE;
import static io.kontur.eventapi.entity.EventType.FLOOD;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static io.kontur.eventapi.util.JsonUtil.readJson;
import static org.apache.commons.lang3.StringUtils.contains;

@Component
public class PdcSqsMessageNormalizer extends PdcHazardNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(PdcSqsMessageNormalizer.class);

    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        if (PDC_SQS_PROVIDER.equals(dataLakeDto.getProvider())) {
            Map<String, Object> props = parseProps(parseEvent(dataLakeDto.getData()));
            return !(FLOOD.equals(defineType(readString((Map<String, Object>) props.get("hazardType"), "typeId")))
                    && contains(readString((Map<String, Object>) props.get("hazardDescription"), "description"), ORIGIN_NASA));
        }
        return false;
    }

    @Override
    public NormalizedObservation runNormalization(DataLake dataLakeDto) {
        JsonNode event = parseEvent(dataLakeDto.getData());
        String type = event.get("syncDa").get("masterSyncEvents").get("type").asText();

        NormalizedObservation normalizedDto = new NormalizedObservation();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());

        Map<String, Object> props = parseProps(event);
        normalizedDto.setSourceUpdatedAt(readDateTime(props, "updateDate"));

        switch (type) {
            case "MAG":
                String uniqueExternalId = event.get("syncDa").get("masterSyncEvents").get("uuid").asText();
                convertMagTypeProperties(normalizedDto, props, uniqueExternalId);
                break;
            case "HAZARD":
                convertHazardTypeProperties(normalizedDto, props);
                break;
            default:
                throw new IllegalArgumentException("Unexpected message type: " + type);
        }
        return normalizedDto;
    }

    protected JsonNode parseEvent(String data) {
        JsonNode sns = JsonUtil.readTree(data).get("Sns");
        JsonNode message = JsonUtil.readTree(sns.get("Message").asText());
        return JsonUtil.readTree(message.get("event").asText());
    }

    protected Map<String, Object> parseProps(JsonNode event) {
        return readJson(event.get("json").asText(), new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private void convertMagTypeProperties(NormalizedObservation normalizedDto, Map<String, Object> props, String uniqueExternalId) {
        normalizedDto.setExternalEpisodeId(uniqueExternalId);
        convertHazardTypeProperties(normalizedDto, (Map<String, Object>) props.get("hazard"));
        normalizedDto.setActive(readBoolean(props, "isActive"));
        normalizedDto.setGeometries(convertMagGeometry(props));
    }

    @SuppressWarnings("unchecked")
    private void convertHazardTypeProperties(NormalizedObservation normalizedDto, Map<String, Object> props) {
        normalizedDto.setExternalEventId(readString(props, "uuid"));
        normalizedDto.setName(readString(props, "hazardName"));
        String description = readString((Map<String, Object>) props.get("hazardDescription"), "description");
        normalizedDto.setDescription(description);
        normalizedDto.setEpisodeDescription(description);
        String origin = description != null && description.contains(ORIGIN_NASA) ? ORIGIN_NASA : null;
        normalizedDto.setOrigin(origin);
        normalizedDto.setStartedAt(readDateTime(props, "startDate"));
        normalizedDto.setEndedAt(readDateTime(props, "endDate"));
        normalizedDto.setEventSeverity(
                defineSeverity(readString((Map<String, Object>) props.get("hazardSeverity"), "severityId")));
        normalizedDto.setType(defineType(readString((Map<String, Object>) props.get("hazardType"), "typeId")));
        Map<String, Object> hazardSnc = (Map<String, Object>) props.get("hazardSnc");
        String url = hazardSnc == null ? null : readString(hazardSnc, "sncUrl");
        if (StringUtils.isNotBlank(url)) {
            normalizedDto.setUrls(List.of(url));
        }
        String pointWkt = makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude"));
        normalizedDto.setPoint(pointWkt);

        if(normalizedDto.getExternalEpisodeId() == null) {
            normalizedDto.setExternalEpisodeId(readString(props, "uuid"));
        }

        try {
            normalizedDto.setGeometries(convertHazardGeometry(pointWkt, normalizedDto.getType()));
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection convertMagGeometry(Map<String, Object> props) {
        try {
            Geometry wktGeometry = wktReader.read((String) ((Map<String, Object>) props.get("wkt")).get("text"));
            org.wololo.geojson.Geometry geoJsonGeometry = geoJSONWriter.write(wktGeometry);
            return new FeatureCollection(new Feature[]{new Feature(geoJsonGeometry, MAG_PROPERTIES)});
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private FeatureCollection convertHazardGeometry(String point, EventType type) throws ParseException {
        org.wololo.geojson.Geometry geometry = geoJSONWriter.write(wktReader.read(point));
        Feature feature = new Feature(geometry, type == CYCLONE ? SQS_CYCLONE_PROPERTIES : HAZARD_PROPERTIES);
        return new FeatureCollection(new Feature[] {feature});
    }
}
