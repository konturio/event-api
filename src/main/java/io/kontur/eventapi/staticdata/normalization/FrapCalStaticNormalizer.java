package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.MultiPolygon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
public class FrapCalStaticNormalizer extends StaticNormalizer {
    @Override
    protected List<String> getProviders() {
        return List.of("wildfire.frap.cal");
    }

    @Override
    protected void setExtraFields(DataLake dataLake, NormalizedObservation normalizedObservation) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLake.getData());
        Map<String, Object> properties = feature.getProperties();

        MultiPolygon multiPolygon = (MultiPolygon) feature.getGeometry();
        Double lon = multiPolygon.getCoordinates()[0][0][0][0];
        Double lat = multiPolygon.getCoordinates()[0][0][0][1];
        normalizedObservation.setPoint(makeWktPoint(lon, lat));
        normalizedObservation.setGeometries(new FeatureCollection(new Feature[] {feature}).toString());

        String state = readString(properties, "STATE");
        String unit = readString(properties, "UNIT_ID");
        normalizedObservation.setName(createName("Wildfire", unit, state, "USA"));

        OffsetDateTime startedAt = parseDate(readString(properties, "ALARM_DATE"));
        OffsetDateTime endedAt = parseDate(readString(properties, "CONT_DATE"));
        OffsetDateTime dateFromYear = createDateFromYear(readInt(properties, "YEAR_"));
        normalizedObservation.setStartedAt(startedAt != null ? startedAt : endedAt != null ? endedAt : dateFromYear);
        normalizedObservation.setEndedAt(endedAt != null ? endedAt : startedAt != null ? startedAt : dateFromYear);

        normalizedObservation.setEventSeverity(Severity.UNKNOWN);
        normalizedObservation.setType(EventType.WILDFIRE);
    }

    private OffsetDateTime parseDate(String str) {
        return StringUtils.isBlank(str) ? null : OffsetDateTime.of(LocalDate.parse(str), LocalTime.MIN, ZoneOffset.UTC);
    }

    private OffsetDateTime createDateFromYear(Integer year) {
        LocalDate localDate = LocalDate.of(year, 1, 1);
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }
}
