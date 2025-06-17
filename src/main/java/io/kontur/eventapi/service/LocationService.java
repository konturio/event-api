package io.kontur.eventapi.service;

import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.lang3.math.NumberUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LocationService {

    private final KonturApiClient konturApiClient;
    private final GeoJSONReader geoJSONReader = new GeoJSONReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public LocationService(KonturApiClient konturApiClient) {
        this.konturApiClient = konturApiClient;
    }

    public String findGaulLocation(Set<NormalizedObservation> observations) {
        List<Geometry> geometries = observations.stream()
                .map(NormalizedObservation::getGeometries)
                .filter(Objects::nonNull)
                .flatMap(fc -> Arrays.stream(fc.getFeatures()))
                .map(Feature::getGeometry)
                .map(geoJSONReader::read)
                .collect(Collectors.toList());
        if (geometries.isEmpty()) {
            return null;
        }
        Point centroid = geometryFactory.buildGeometry(geometries).getCentroid();
        Map<String, Object> params = new HashMap<>();
        params.put("geometry", geoJSONWriter.write(centroid));
        params.put("limit", 10);
        FeatureCollection adminBoundaries = konturApiClient.adminBoundaries(params);
        if (adminBoundaries == null || adminBoundaries.getFeatures() == null) {
            return null;
        }
        return Stream.of(adminBoundaries.getFeatures())
                .map(Feature::getProperties)
                .filter(p -> NumberUtils.isCreatable(String.valueOf(p.get("admin_level"))))
                .sorted(Comparator.comparing(p -> Double.parseDouble(String.valueOf(p.get("admin_level")))))
                .limit(3)
                .map(p -> {
                    Map<String, String> tags = (Map<String, String>) p.get("tags");
                    if (tags.containsKey("name:en")) {
                        return tags.get("name:en");
                    } else if (tags.containsKey("int_name")) {
                        return tags.get("int_name");
                    } else {
                        return tags.get("name");
                    }
                })
                .collect(Collectors.joining(", "));
    }
}
