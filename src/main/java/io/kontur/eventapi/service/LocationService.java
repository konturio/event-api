package io.kontur.eventapi.service;

import io.kontur.eventapi.client.KonturApiClient;
import org.apache.commons.lang3.math.NumberUtils;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LocationService {
    private static final Logger LOG = LoggerFactory.getLogger(LocationService.class);

    private final KonturApiClient konturApiClient;
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public LocationService(KonturApiClient konturApiClient) {
        this.konturApiClient = konturApiClient;
    }

    public String getLocation(Double lon, Double lat) {
        if (lon == null || lat == null) {
            return null;
        }
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        Map<String, Object> params = new HashMap<>();
        params.put("geometry", geoJSONWriter.write(point));
        params.put("limit", 10);

        FeatureCollection adminBoundaries = null;
        try {
            adminBoundaries = konturApiClient.adminBoundaries(params);
        } catch (Exception e) {
            LOG.warn("Failed to call admin boundaries service: {}", e.getMessage());
        }
        if (adminBoundaries == null || adminBoundaries.getFeatures() == null) {
            return null;
        }
        return Stream.of(adminBoundaries.getFeatures())
                .map(Feature::getProperties)
                .filter(p -> NumberUtils.isCreatable(String.valueOf(p.get("admin_level"))))
                .sorted(Comparator.comparing(p -> Double.parseDouble(String.valueOf(p.get("admin_level")))))
                .limit(3)
                .map(p -> (Map<String, String>) p.get("tags"))
                .map(tags -> {
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
