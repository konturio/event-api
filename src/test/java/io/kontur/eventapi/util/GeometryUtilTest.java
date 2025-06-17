package io.kontur.eventapi.util;

import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Polygon;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeometryUtilTest {

    @Test
    void testIsValid() {
        double[][][] coords = {{{0,0},{0,1},{1,1},{1,0},{0,0}}};
        FeatureCollection fc = GeometryUtil.convertGeometryToFeatureCollection(new Polygon(coords), Map.of());
        assertTrue(GeometryUtil.isValid(fc));

        double[][][] wrong = {{{0,0},{0,1},{1,1},{1,0}}};
        FeatureCollection invalid = GeometryUtil.convertGeometryToFeatureCollection(new Polygon(wrong), Map.of());
        assertFalse(GeometryUtil.isValid(invalid));
    }
}
