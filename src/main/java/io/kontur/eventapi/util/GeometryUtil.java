package io.kontur.eventapi.util;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.*;

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

    /**
     * Calculate geodesic length on the WGS84 ellipsoid in kilometres.
     * <p>Designed for linear geometries (LineString/MultiLineString). When a
     * polygon is supplied its exterior ring is measured; for area calculations
     * prefer {@link #calculateAreaKm2(Geometry)}.</p>
     * <p>Returns {@code 0.0} when the geometry is {@code null}, empty or
     * contains no segments. Coordinate sequences shorter than two points are
     * skipped.</p>
     *
     * @param geometry geometry to measure
     * @return length in kilometres
     */
    public static double calculateLengthKm(Geometry geometry) {
        if (geometry == null || geometry.getNumGeometries() == 0) {
            return 0d;
        }
        double lengthInMeters = 0d;
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry g = geometry.getGeometryN(i);
            var coords = g instanceof Polygon
                    ? ((Polygon) g).getExteriorRing().getCoordinates()
                    : g.getCoordinates();
            if (coords == null || coords.length < 2) {
                continue;
            }
            for (int j = 1; j < coords.length; j++) {
                lengthInMeters += Geodesic.WGS84.Inverse(
                        coords[j - 1].y, coords[j - 1].x,
                        coords[j].y, coords[j].x
                ).s12;
            }
        }
        return lengthInMeters / 1_000d;
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
        if (fc1 == null || fc2 == null) {
            return fc1 == null && fc2 == null;
        }
        Feature[] features1 = fc1.getFeatures();
        Feature[] features2 = fc2.getFeatures();
        if (features1.length != features2.length) return false;

        // cache parsed geometries for features2 to avoid repeated reader.read() calls
        Map<Feature, Geometry> geomCache = new HashMap<>();
        for (Feature f : features2) {
            org.wololo.geojson.Geometry g = f.getGeometry();
            Geometry geom = null;
            if (g != null) {
                geom = reader.read(g);
                if (!IsValidOp.isValid(geom)) {
                    geom = geom.buffer(0);
                }
            }
            geomCache.put(f, geom);
        }

        List<Feature> remaining = new ArrayList<>(Arrays.asList(features2));
        outer: for (Feature feature1 : features1) {
            org.wololo.geojson.Geometry g1 = feature1.getGeometry();
            if (g1 == null) {
                for (Iterator<Feature> it = remaining.iterator(); it.hasNext(); ) {
                    Feature feature2 = it.next();
                    if (!Objects.equals(feature1.getProperties(), feature2.getProperties())) continue;
                    if (geomCache.get(feature2) == null) {
                        it.remove();
                        geomCache.remove(feature2);
                        continue outer;
                    }
                }
                return false;
            }
            Geometry geom1 = reader.read(g1);
            if (!IsValidOp.isValid(geom1)) {
                geom1 = geom1.buffer(0);
            }
            geom1.normalize();
            for (Iterator<Feature> it = remaining.iterator(); it.hasNext(); ) {
                Feature feature2 = it.next();
                if (!Objects.equals(feature1.getProperties(), feature2.getProperties())) continue;
                Geometry geom2 = geomCache.get(feature2);
                if (geom2 == null) continue;
                geom2.normalize();
                if (geom1.equalsTopo(geom2) || geom1.equalsExact(geom2, 0.0001)) {
                    it.remove();
                    geomCache.remove(feature2);
                    continue outer;
                }
            }
            return false;
        }
        return remaining.isEmpty();
    }
}
