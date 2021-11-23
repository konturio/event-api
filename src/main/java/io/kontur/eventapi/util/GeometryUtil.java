package io.kontur.eventapi.util;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import org.locationtech.jts.geom.Geometry;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.util.Arrays;
import java.util.Collections;

public class GeometryUtil {

    public static Double calculateAreaKm2(Geometry geometry) {
        double areaInMeters = 0;
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            PolygonArea polygonArea = new PolygonArea(Geodesic.WGS84, false);
            Arrays.stream(geometry.getGeometryN(i).getCoordinates()).forEach(c -> polygonArea.AddPoint(c.getY(), c.getX()));
            areaInMeters += Math.abs(polygonArea.Compute().area);
        }
        double areaInKm = areaInMeters / 1_000_000;
        return areaInKm;
    }

    public static FeatureCollection convertFeatureToFeatureCollection(Feature feature) {
        Feature featureWithoutProperties = new Feature(feature.getGeometry(), Collections.emptyMap());
        return new FeatureCollection(new Feature[] {featureWithoutProperties});
    }

    public static Feature readFeature(String featureString) {
        return (Feature) GeoJSONFactory.create(featureString);
    }
}
