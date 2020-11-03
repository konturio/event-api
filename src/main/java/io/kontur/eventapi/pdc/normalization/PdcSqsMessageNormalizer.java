package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.util.JsonUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.util.HashMap;
import java.util.Map;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static io.kontur.eventapi.util.JsonUtil.readJson;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class PdcSqsMessageNormalizer extends PdcHazardNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(PdcSqsMessageNormalizer.class);

    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return PDC_SQS_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        JsonNode sns = JsonUtil.readTree(dataLakeDto.getData()).get("Sns");
        JsonNode message = JsonUtil.readTree(sns.get("Message").asText());
        JsonNode event = JsonUtil.readTree(message.get("event").asText());
        String type = event.get("syncDa").get("masterSyncEvents").get("type").asText();

        NormalizedObservation normalizedDto = new NormalizedObservation();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());

        Map<String, Object> props = readJson(event.get("json").asText(), new TypeReference<>() {});
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

    @SuppressWarnings("unchecked")
    private void convertMagTypeProperties(NormalizedObservation normalizedDto, Map<String, Object> props, String uniqueExternalId) {
        normalizedDto.setExternalUniqueEventId(uniqueExternalId);
        convertHazardTypeProperties(normalizedDto, (Map<String, Object>) props.get("hazard"));
        normalizedDto.setActive(readBoolean(props, "isActive"));
        normalizedDto.setGeometries(writeJson(convertGeometries(props)));
    }

    @SuppressWarnings("unchecked")
    private void convertHazardTypeProperties(NormalizedObservation normalizedDto, Map<String, Object> props) {
        normalizedDto.setExternalEventId(readString(props, "uuid"));
        normalizedDto.setName(readString(props, "hazardName"));
        String description = readString((Map<String, Object>) props.get("hazardDescription"), "description");
        normalizedDto.setDescription(description);
        normalizedDto.setEpisodeDescription(description);
        normalizedDto.setStartedAt(readDateTime(props, "startDate"));
        normalizedDto.setEndedAt(readDateTime(props, "endDate"));
        normalizedDto.setEventSeverity(
                defineSeverity(readString((Map<String, Object>) props.get("hazardSeverity"), "severityId")));
        normalizedDto.setType(defineType(readString((Map<String, Object>) props.get("hazardType"), "typeId")));
        String pointWkt = makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude"));
        normalizedDto.setPoint(pointWkt);

        if(normalizedDto.getExternalUniqueEventId() == null) {
            normalizedDto.setExternalUniqueEventId(readString(props, "uuid"));
        }

        try {
            normalizedDto.setGeometries(writeJson(convertGeometry(pointWkt, props)));
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection convertGeometries(Map<String, Object> props) {
        try {
            Geometry wktGeometry = wktReader.read((String) ((Map<String, Object>) props.get("wkt")).get("text"));
            org.wololo.geojson.Geometry geoJsonGeometry = geoJSONWriter.write(wktGeometry);
            Map<String, Object> map = new HashMap<>();

            map.put("description", convertDescription(props));
            map.put("active", readBoolean(props, "isActive"));
            map.put("updatedAt", readDateTime(props, "updateDate"));
            return new FeatureCollection(new Feature[]{new Feature(geoJsonGeometry, map)});
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection convertGeometry(String point, Map<String, Object> props) throws ParseException {
        org.wololo.geojson.Geometry geometry = geoJSONWriter.write(wktReader.read(point));

        Map<String, Object> map = new HashMap<>();
        map.put("description", readString((Map<String, Object>) props.get("hazardDescription"), "description"));
        map.put("updatedAt", readDateTime(props, "updateDate"));

        Feature feature = new Feature(geometry, map);

        return new FeatureCollection(new Feature[] {feature});
    }

    @SuppressWarnings("unchecked")
    private String convertDescription(Map<String, Object> props) {
        Map<String, Object> hazardProperties = (Map<String, Object>) props.get("hazard");
        return readString((Map<String, Object>) hazardProperties.get("hazardDescription"), "description");
    }
}
