package io.kontur.eventapi.staticdata.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.util.GeometryUtil.convertGeometryToFeatureCollection;

@Component
public class FrapCalStaticNormalizer extends StaticNormalizer {

    private final static GeoJSONReader reader = new GeoJSONReader();

    @Override
    protected List<String> getProviders() {
        return List.of("wildfire.frap.cal");
    }

    @Override
    protected void setExtraFields(DataLake dataLake, NormalizedObservation normalizedObservation) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLake.getData());
        Map<String, Object> properties = feature.getProperties();

        MultiPolygon multiPolygon = (MultiPolygon) reader.read(feature.getGeometry());
        Point point = multiPolygon.getCentroid();
        normalizedObservation.setPoint(makeWktPoint(point.getX(), point.getY()));
        normalizedObservation.setGeometries(convertGeometryToFeatureCollection(feature.getGeometry(), WILDFIRE_PROPERTIES));

        String state = readString(properties, "STATE");
        String unit = readString(properties, "UNIT_ID");
        normalizedObservation.setName(createName("Wildfire", unit, state, "USA"));
        normalizedObservation.setProperName(readString(properties, "FIRE_NAME"));

        OffsetDateTime startedAt = parseLocalDate(readString(properties, "ALARM_DATE"));
        OffsetDateTime endedAt = parseLocalDate(readString(properties, "CONT_DATE"));
        OffsetDateTime dateFromYear = createDateFromYear(readInt(properties, "YEAR_"));
        normalizedObservation.setStartedAt(startedAt != null ? startedAt : endedAt != null ? endedAt : dateFromYear);
        normalizedObservation.setEndedAt(endedAt != null ? endedAt : startedAt != null ? startedAt : dateFromYear);

        normalizedObservation.setEventSeverity(Severity.UNKNOWN);
        normalizedObservation.setType(EventType.WILDFIRE);
    }

    private OffsetDateTime createDateFromYear(Integer year) {
        LocalDate localDate = LocalDate.of(year, 1, 1);
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }
}
