package io.kontur.eventapi.swissre.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import org.springframework.stereotype.Component;
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

import static io.kontur.eventapi.swissre.util.StaticTornadoUtil.*;

@Component
public class StaticTornadoNormalizer extends Normalizer {
    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return PROVIDERS.containsKey(dataLakeDto.getProvider());
    }

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

        normalizedObservation.setEventSeverity(getSeverity((String) properties.get("fujita_scale")));
        normalizedObservation.setCost(getCost(properties.get("damage_property")));

        String nearestCity = (String) properties.get("nearest_city");
        String state = (String) properties.get("admin0");
        normalizedObservation.setName(createName(dataLakeDto.getProvider(), nearestCity, state));

        String updatedAt = SOURCE_UPDATES.get(dataLakeDto.getProvider());
        normalizedObservation.setSourceUpdatedAt(parseDate(updatedAt));

        Double latitude = objectToDouble(properties.get("latitude"));
        Double longitude = objectToDouble(properties.get("longitude"));
        normalizedObservation.setPoint(makeWktPoint(longitude, latitude));
        normalizedObservation.setGeometries(new FeatureCollection(new Feature[]{feature}).toString());

        return normalizedObservation;
    }

    private Severity getSeverity(String fujitaScale) {
        if (fujitaScale == null || !SEVERITIES.containsKey(fujitaScale)) {
            return Severity.UNKNOWN;
        }
        return SEVERITIES.get(fujitaScale);
    }

    private BigDecimal getCost(Object damage) {
        if (damage == null || UNKNOWN.equals(damage)) {
            return null;
        }
        return BigDecimal.valueOf(Integer.parseInt((String) damage));
    }

    private Double objectToDouble(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        } else {
            return Double.valueOf((Integer) obj);
        }
    }

    private String createName(String provider, String nearestCity, String state) {
        StringBuilder sb = new StringBuilder("Tornado - ");
        sb.append(nearestCity).append(", ");
        if (provider.equals(CANADA_GOV)) {
            sb.append(state).append(", ");
        }
        sb.append(COUNTRY_NAMES.get(provider));
        return sb.toString();
    }

    private OffsetDateTime parseDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE);
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }
}
