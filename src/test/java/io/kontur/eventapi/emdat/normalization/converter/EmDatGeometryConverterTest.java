package io.kontur.eventapi.emdat.normalization.converter;

import org.junit.jupiter.api.Test;
import org.wololo.geojson.*;

import java.util.Map;

import static io.kontur.eventapi.util.GeometryUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class EmDatGeometryConverterTest {

    private static final EmDatGeometryConverter converter = new EmDatGeometryConverter();

    @Test
    public void testConvertGeometry_Magnitude() {
        Geometry geom = createGeometry();
        Point point = createPoint();
        FeatureCollection fc = converter.convertGeometry(geom, point, "Richter", 5);

        assertEquals(2, fc.getFeatures().length);

        Feature geomFeature = fc.getFeatures()[0];
        assertEquals(geom, geomFeature.getGeometry());
        assertEquals(3, geomFeature.getProperties().size());
        assertEquals(5, ((Map<String, Object>) geomFeature.getProperties().get(SEVERITY_DATA_PROPERTY)).get(MAGNITUDE));
        assertEquals(GLOBAL_AREA, geomFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, geomFeature.getProperties().get(IS_OBSERVED_PROPERTY));

        Feature pointFeature = fc.getFeatures()[1];
        assertEquals(point, pointFeature.getGeometry());
        assertEquals(3, pointFeature.getProperties().size());
        assertEquals(5, ((Map<String, Object>) pointFeature.getProperties().get(SEVERITY_DATA_PROPERTY)).get(MAGNITUDE));
        assertEquals(POSITION, pointFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, pointFeature.getProperties().get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertGeometry_WindSpeed() {
        Geometry geom = createGeometry();
        Point point = createPoint();
        FeatureCollection fc = converter.convertGeometry(geom, point, "Kph", 100);

        assertEquals(2, fc.getFeatures().length);

        Feature geomFeature = fc.getFeatures()[0];
        assertEquals(geom, geomFeature.getGeometry());
        assertEquals(3, geomFeature.getProperties().size());
        assertEquals(100, ((Map<String, Object>) geomFeature.getProperties().get(SEVERITY_DATA_PROPERTY)).get(WIND_SPEED_KPH));
        assertEquals(GLOBAL_AREA, geomFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, geomFeature.getProperties().get(IS_OBSERVED_PROPERTY));

        Feature pointFeature = fc.getFeatures()[1];
        assertEquals(point, pointFeature.getGeometry());
        assertEquals(3, pointFeature.getProperties().size());
        assertEquals(100, ((Map<String, Object>) pointFeature.getProperties().get(SEVERITY_DATA_PROPERTY)).get(WIND_SPEED_KPH));
        assertEquals(POSITION, pointFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, pointFeature.getProperties().get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertGeometry_UnknownSeverityUnit() {
        Geometry geom = createGeometry();
        Point point = createPoint();
        FeatureCollection fc = converter.convertGeometry(geom, point, "Km2", 100);

        assertEquals(2, fc.getFeatures().length);

        Feature geomFeature = fc.getFeatures()[0];
        assertEquals(geom, geomFeature.getGeometry());
        assertEquals(2, geomFeature.getProperties().size());
        assertNull(geomFeature.getProperties().get(SEVERITY_DATA_PROPERTY));
        assertEquals(GLOBAL_AREA, geomFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, geomFeature.getProperties().get(IS_OBSERVED_PROPERTY));

        Feature pointFeature = fc.getFeatures()[1];
        assertEquals(point, pointFeature.getGeometry());
        assertEquals(2, pointFeature.getProperties().size());
        assertNull(pointFeature.getProperties().get(SEVERITY_DATA_PROPERTY));
        assertEquals(POSITION, pointFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, pointFeature.getProperties().get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertGeometry_NoPoint() {
        Geometry geom = createGeometry();
        FeatureCollection fc = converter.convertGeometry(geom, null, "Kph", 100);

        assertEquals(1, fc.getFeatures().length);

        Feature geomFeature = fc.getFeatures()[0];
        assertEquals(geom, geomFeature.getGeometry());
        assertEquals(3, geomFeature.getProperties().size());
        assertEquals(100, ((Map<String, Object>) geomFeature.getProperties().get(SEVERITY_DATA_PROPERTY)).get(WIND_SPEED_KPH));
        assertEquals(GLOBAL_AREA, geomFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, geomFeature.getProperties().get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertGeometry_NoGeometry() {
        Point point = createPoint();
        FeatureCollection fc = converter.convertGeometry(null, point, "Kph", 100);

        assertEquals(1, fc.getFeatures().length);

        Feature pointFeature = fc.getFeatures()[0];
        assertEquals(point, pointFeature.getGeometry());
        assertEquals(3, pointFeature.getProperties().size());
        assertEquals(100, ((Map<String, Object>) pointFeature.getProperties().get(SEVERITY_DATA_PROPERTY)).get(WIND_SPEED_KPH));
        assertEquals(POSITION, pointFeature.getProperties().get(AREA_TYPE_PROPERTY));
        assertEquals(true, pointFeature.getProperties().get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertGeometry_NoGeometryAndNoPoint() {
        FeatureCollection fc = converter.convertGeometry(null, null, "Kph", 100);
        assertNull(fc);
    }

    private Geometry createGeometry() {
        return new Polygon(new double[][][] {{{0, 0}, {0, 10}, {10, 10}, {10, 0}, {10, 0}}});
    }

    private Point createPoint() {
        return new Point(new double[] {0, 0});
    }
}