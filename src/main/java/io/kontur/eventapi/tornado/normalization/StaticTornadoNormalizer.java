package io.kontur.eventapi.tornado.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import org.apache.commons.lang3.math.NumberUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public abstract class StaticTornadoNormalizer extends Normalizer {

    private final static Map<String, Severity> SEVERITIES = Map.of(
            "0", Severity.MINOR,
            "1", Severity.MODERATE,
            "2", Severity.MODERATE,
            "3", Severity.SEVERE,
            "4", Severity.EXTREME,
            "5", Severity.EXTREME
    );

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setType(EventType.TORNADO);
        normalizedObservation.setActive(false);
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setProvider(dataLakeDto.getProvider());

        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        Map<String, Object> properties = feature.getProperties();

        normalizedObservation.setExternalEventId((String) properties.get("source_id"));
        normalizedObservation.setDescription((String) properties.get("comments"));

        OffsetDateTime date = parseDate((String) properties.get("date"));
        normalizedObservation.setStartedAt(date);
        normalizedObservation.setEndedAt(date);

        normalizedObservation.setEventSeverity(convertSeverity((String) properties.get("fujita_scale")));

        String damage = (String) properties.get("damage_property");
        normalizedObservation.setCost(NumberUtils.isParsable(damage) ? BigDecimal.valueOf(Long.parseLong(damage)) : null);

        Double latitude = objectToDouble(properties.get("latitude"));
        Double longitude = objectToDouble(properties.get("longitude"));
        normalizedObservation.setPoint(makeWktPoint(longitude, latitude));
        normalizedObservation.setGeometries(new FeatureCollection(new Feature[]{feature}).toString());

        normalizedObservation.setName(createName(properties));
        normalizedObservation.setSourceUpdatedAt(parseDate(getSourceUpdatedAt()));

        return normalizedObservation;
    }

    private Severity convertSeverity(String fujitaScale) {
        if (fujitaScale != null && SEVERITIES.containsKey(fujitaScale)) {
            return SEVERITIES.get(fujitaScale);
        }
        return Severity.UNKNOWN;
    }

    private Double objectToDouble(Object obj) {
        return obj instanceof Double ? (Double) obj : Double.valueOf((Integer) obj);
    }

    protected OffsetDateTime parseDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE);
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }

    protected abstract String createName(Map<String, Object> properties);
    protected abstract String getSourceUpdatedAt();
}
