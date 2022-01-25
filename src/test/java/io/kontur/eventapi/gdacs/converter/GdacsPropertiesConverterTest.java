package io.kontur.eventapi.gdacs.converter;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static io.kontur.eventapi.util.GeometryUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class GdacsPropertiesConverterTest {

    private final GdacsPropertiesConverter converter = new GdacsPropertiesConverter();
    private final OffsetDateTime polygonDate = OffsetDateTime.parse("2020-01-01T00:00:00Z");
    private final String polygonLabel = "label";

    @Test
    public void testConvertProperties_Poly_Cones_0() {
        Map<String, Object> props = converter.convertProperties("Poly_Cones_0", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(EXPOSURE, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Cones_6() {
        Map<String, Object> props = converter.convertProperties("Poly_Cones_6", polygonDate, polygonLabel, false);

        assertEquals(4, props.size());
        assertEquals(EXPOSURE, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(6, props.get(FORECAST_HRS_PROPERTY));
        assertEquals(OffsetDateTime.parse("2020-01-01T06:00:00Z"), props.get(TIMESTAMP_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Cones_12() {
        Map<String, Object> props = converter.convertProperties("Poly_Cones_12", polygonDate, polygonLabel, false);

        assertEquals(4, props.size());
        assertEquals(EXPOSURE, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(12, props.get(FORECAST_HRS_PROPERTY));
        assertEquals(OffsetDateTime.parse("2020-01-01T12:00:00Z"), props.get(TIMESTAMP_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Cones_18() {
        Map<String, Object> props = converter.convertProperties("Poly_Cones_18", polygonDate, polygonLabel, false);

        assertEquals(4, props.size());
        assertEquals(EXPOSURE, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(18, props.get(FORECAST_HRS_PROPERTY));
        assertEquals(OffsetDateTime.parse("2020-01-01T18:00:00Z"), props.get(TIMESTAMP_PROPERTY));
    }

    @Test
    public void testConvertProperties_Point_Centroid() {
        Map<String, Object> props = converter.convertProperties("Point_Centroid", polygonDate, polygonLabel, false);

        assertEquals(1, props.size());
        assertEquals(CENTER_POINT, props.get(AREA_TYPE_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Circle() {
        Map<String, Object> props = converter.convertProperties("Poly_Circle", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(ALERT_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Cones() {
        Map<String, Object> props = converter.convertProperties("Poly_Cones", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(ALERT_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Point_Polygon_Point_N() {
        Map<String, Object> props = converter.convertProperties("Point_Polygon_Point_1", OffsetDateTime.parse("2020-01-01T00:00:00Z"), "01/01 00:00 UTC", false);

        assertEquals(2, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));

        props = converter.convertProperties("Point_Polygon_Point_1", OffsetDateTime.parse("2020-01-01T00:00:00Z"), "30/12 00:00 UTC", false);

        assertEquals(2, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));


        props = converter.convertProperties("Point_Polygon_Point_1", OffsetDateTime.parse("2020-12-31T23:00:00Z"), "01/01 00:00 UTC", false);

        assertEquals(4, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(1L, props.get(FORECAST_HRS_PROPERTY));
        assertEquals(OffsetDateTime.parse("2021-01-01T00:00:00Z"), props.get(TIMESTAMP_PROPERTY));

        props = converter.convertProperties("Point_Polygon_Point_1", OffsetDateTime.parse("2020-09-01T00:00:00Z"), "01/03 00:00 UTC", false);

        assertEquals(2, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));

        props = converter.convertProperties("Point_Polygon_Point_1", OffsetDateTime.parse("2020-10-01T00:00:00Z"), "01/03 00:00 UTC", false);

        assertEquals(4, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(3624L, props.get(FORECAST_HRS_PROPERTY));
        assertEquals(OffsetDateTime.parse("2021-03-01T00:00:00Z"), props.get(TIMESTAMP_PROPERTY));

        props = converter.convertProperties("Point_Polygon_Point_1", OffsetDateTime.parse("2020-10-01T00:00:00Z"), "not parsable date", false);

        assertEquals(2, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Line_Line_N() {
        Map<String, Object> props = converter.convertProperties("Line_Line_1", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(TRACK, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));

        props = converter.convertProperties("Line_Line_1", polygonDate, polygonLabel, true);

        assertEquals(2, props.size());
        assertEquals(TRACK, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Green() {
        Map<String, Object> props = converter.convertProperties("Poly_Green", polygonDate, polygonLabel, false);

        assertEquals(3, props.size());
        assertEquals(ALERT_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(Map.of(WIND_SPEED_KPH, 60), props.get(SEVERITY_DATA_PROPERTY));

        props = converter.convertProperties("Poly_Orange", polygonDate, polygonLabel, false);

        assertEquals(3, props.size());
        assertEquals(ALERT_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(Map.of(WIND_SPEED_KPH, 90), props.get(SEVERITY_DATA_PROPERTY));

        props = converter.convertProperties("Poly_Red", polygonDate, polygonLabel, false);

        assertEquals(3, props.size());
        assertEquals(ALERT_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(false, props.get(IS_OBSERVED_PROPERTY));
        assertEquals(Map.of(WIND_SPEED_KPH, 120), props.get(SEVERITY_DATA_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_area() {
        Map<String, Object> props = converter.convertProperties("Poly_area", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(EXPOSURE, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Global() {
        Map<String, Object> props = converter.convertProperties("Poly_Global", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(GLOBAL_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Point_Global() {
        Map<String, Object> props = converter.convertProperties("Point_Global", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(GLOBAL_POINT, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Intensity() {
        Map<String, Object> props = converter.convertProperties("Poly_Intensity", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(ALERT_AREA, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Point_Affected() {
        Map<String, Object> props = converter.convertProperties("Point_Affected", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(POSITION, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_Poly_Affected() {
        Map<String, Object> props = converter.convertProperties("Poly_Affected", polygonDate, polygonLabel, false);

        assertEquals(2, props.size());
        assertEquals(EXPOSURE, props.get(AREA_TYPE_PROPERTY));
        assertEquals(true, props.get(IS_OBSERVED_PROPERTY));
    }

    @Test
    public void testConvertProperties_ClassNull() {
        Map<String, Object> props = converter.convertProperties(null, polygonDate, polygonLabel, false);
        assertEquals(1, props.size());
        assertEquals(UNKNOWN, props.get(AREA_TYPE_PROPERTY));
    }

    @Test
    public void testConvertProperties_ClassUnknown() {
        Map<String, Object> props = converter.convertProperties("Unknown_Class", polygonDate, polygonLabel, false);
        assertEquals(1, props.size());
        assertEquals(UNKNOWN, props.get(AREA_TYPE_PROPERTY));
    }

    @Test
    public void testMigratedProperties() {
        Map<String, Object> testProps = Map.of(
                "Class", "class",
                "eventid", 101010,
                "eventtype", "VO",
                "eventname", "event name",
                "country", "country",
                "polygondate", "2020-01-01T00:00:00",
                "polygonlabel", "label"
        );
        Map<String, Object> props = new HashMap<>();
        converter.migrateProperties(testProps, props);

        assertEquals("class", props.get("Class"));
        assertEquals(101010, props.get("eventid"));
        assertEquals("VO", props.get("eventtype"));
        assertEquals("event name", props.get("eventname"));
        assertEquals("country", props.get("country"));
        assertEquals("2020-01-01T00:00:00", props.get("polygondate"));
        assertEquals("label", props.get("polygonlabel"));
    }
}