package io.kontur.eventapi.emdat.normalization.converter;

import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.geojson.Point;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.GeometryUtil.POSITION;

@Component
public class EmDatGeometryConverter {

    private static final Map<String, String> severityUnitMap = Map.of(
            "Richter", MAGNITUDE,
            "Kph", WIND_SPEED_KPH
    );

    public FeatureCollection convertGeometry(Geometry geometry, Point point, String severityUnit, Object severityValue) {
        Map<String, Object> severityData = null;
        if (severityUnit != null && severityValue != null && severityUnitMap.containsKey(severityUnit)) {
            severityData = Map.of(severityUnitMap.get(severityUnit), severityValue);
        }

        Feature geomFeature = createFeature(geometry, severityData, GLOBAL_AREA);
        Feature pointFeature = createFeature(point, severityData, POSITION);
        Feature[] features = Stream.of(geomFeature, pointFeature).filter(Objects::nonNull).toArray(Feature[]::new);
        if (features.length == 0) {
            return null;
        }
        return new FeatureCollection(features);
    }

    private Feature createFeature(Geometry geometry, Map<String, Object> severityData, String areaType) {
        if (geometry == null) return null;

        Map<String, Object> props = new HashMap<>();
        props.put(IS_OBSERVED_PROPERTY, true);
        props.put(AREA_TYPE_PROPERTY, areaType);
        if (severityData != null) {
            props.put(SEVERITY_DATA_PROPERTY, severityData);
        }
        return new Feature(geometry, props);
    }
}
