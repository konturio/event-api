package io.kontur.eventapi.util;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class GeometryUtil {

    private final static Logger LOG = LoggerFactory.getLogger(GeometryUtil.class);

    private final static GeoJSONReader reader = new GeoJSONReader();

    public static final String IS_OBSERVED_PROPERTY = "isObserved";
    public static final String FORECAST_HRS_PROPERTY = "forecastHrs";
    public static final String TIMESTAMP_PROPERTY = "timestamp";
    public static final String AREA_TYPE_PROPERTY = "areaType";
    public static final String SEVERITY_DATA_PROPERTY = "severityData";

    public static final String CENTER_POINT = "centerPoint";
    public static final String START_POINT = "startPoint";
    public static final String POSITION = "position";
    public static final String GLOBAL_POINT = "globalPoint";
    public static final String TRACK = "track";
    public static final String ALERT_AREA = "alertArea";
    public static final String EXPOSURE = "exposure";
    public static final String GLOBAL_AREA = "globalArea";
    public static final String UNKNOWN = "unknown";

    public static final String WIND_SPEED_KNOTS = "windSpeedKnots";
    public static final String WIND_SPEED_KPH = "windSpeedKph";
    public static final String WIND_GUSTS_KPH = "windGustsKph";
    public static final String MAGNITUDE = "magnitude";

    public static Double calculateAreaKm2(Geometry geometry) {
        double areaInMeters = 0;
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            PolygonArea polygonArea = new PolygonArea(Geodesic.WGS84, false);
            Arrays.stream(geometry.getGeometryN(i).getCoordinates()).forEach(c -> polygonArea.AddPoint(c.getY(), c.getX()));
            areaInMeters += Math.abs(polygonArea.Compute().area);
        }
        return areaInMeters / 1_000_000;
    }

    public static FeatureCollection convertGeometryToFeatureCollection(org.wololo.geojson.Geometry geometry, Map<String, Object> properties) {
        Feature feature = new Feature(geometry, properties);
        return new FeatureCollection(new Feature[] {feature});
    }

    public static Feature readFeature(String featureString) {
        return (Feature) GeoJSONFactory.create(featureString);
    }

    public static String getCentroid(org.wololo.geojson.Geometry geometry, UUID observationID) {
        try {
            Point centroid = reader.read(geometry).getCentroid();
            return makeWktPoint(centroid.getX(), centroid.getY());
        } catch (Exception e) {
            LOG.debug("Can't find center point for observation. Observation ID: {}", observationID);
        }
        return null;
    }

    protected static String makeWktPoint(Double lon, Double lat) {
        return lon == null || lat == null ? null : String.format("POINT(%s %s)", lon, lat);
    }

    public static boolean isEqualGeometries(FeatureCollection fc1, FeatureCollection fc2) {
        if (fc1.getFeatures().length != fc2.getFeatures().length) return false;
        for (int i = 0; i < fc1.getFeatures().length; i++) {
            Feature feature1 = fc1.getFeatures()[i];
            Feature feature2 = fc2.getFeatures()[i];
            if (!feature1.getProperties().equals(feature2.getProperties())) return false;

            Geometry geom1 = reader.read(feature1.getGeometry());
            Geometry geom2 = reader.read(feature2.getGeometry());

            if (!IsValidOp.isValid(geom1) || !IsValidOp.isValid(geom2)) {
                geom1 = geom1.buffer(0);
                geom2 = geom2.buffer(0);
            }
            if (!geom1.equalsExact(geom2, 0.0001)) return false;
        }
        return true;
    }

    public static boolean intersectsEnvelope(FeatureCollection fc,
                                             double minLon, double minLat,
                                             double maxLon, double maxLat) {
        if (fc == null) {
            return false;
        }
        var gf = new org.locationtech.jts.geom.GeometryFactory();
        var envelope = new org.locationtech.jts.geom.Envelope(minLon, maxLon, minLat, maxLat);
        var bbox = gf.toGeometry(envelope);
        for (Feature feature : fc.getFeatures()) {
            try {
                Geometry geom = reader.read(feature.getGeometry());
                if (!IsValidOp.isValid(geom)) {
                    geom = geom.buffer(0);
                }
                if (geom.intersects(bbox)) {
                    return true;
                }
            } catch (Exception e) {
                LOG.debug("Failed intersection check: {}", e.getMessage());
            }
        }
        return false;
    }
}
