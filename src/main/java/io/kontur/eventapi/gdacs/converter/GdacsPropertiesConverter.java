package io.kontur.eventapi.gdacs.converter;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;

import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.GeometryUtil.IS_OBSERVED_PROPERTY;
import static io.kontur.eventapi.util.SeverityUtil.*;
import static java.lang.Integer.parseInt;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.Collections.singletonMap;
import static java.util.Map.entry;
import static org.apache.commons.lang3.StringUtils.getDigits;

@Component
public class GdacsPropertiesConverter {

    private final Map<String, String> areaTypeMap = Map.ofEntries(
            entry("Poly_Cones_", EXPOSURE),
            entry("Poly_area", EXPOSURE),
            entry("Poly_Affected", EXPOSURE),
            entry("Poly_Circle", ALERT_AREA),
            entry("Poly_Intensity", ALERT_AREA),
            entry("Poly_Cones", ALERT_AREA),
            entry("Poly_Green", ALERT_AREA),
            entry("Poly_Orange", ALERT_AREA),
            entry("Poly_Red", ALERT_AREA),
            entry("Poly_Global", GLOBAL_AREA),
            entry("Line_Line_", TRACK),
            entry("Point_Centroid", CENTER_POINT),
            entry("Point_Polygon_Point_", POSITION),
            entry("Point_Affected", POSITION),
            entry("Point_Global", GLOBAL_POINT)
    );

    private final Map<String, Boolean> isObservedMap = Map.ofEntries(
            entry("Poly_Cones_0", true),
            entry("Poly_area", true),
            entry("Poly_Affected", true),
            entry("Poly_Cones_6", false),
            entry("Poly_Cones_12", false),
            entry("Poly_Cones_18", false),
            entry("Poly_Circle", true),
            entry("Poly_Intensity", true),
            entry("Poly_Cones", false),
            entry("Poly_Green", false),
            entry("Poly_Orange", false),
            entry("Poly_Red", false),
            entry("Poly_Global", true),
            entry("Point_Affected", true),
            entry("Point_Global", true)
    );

    private final Map<String, Map<String, Object>> severityDataMap = Map.ofEntries(
            entry("Poly_Green", Map.of(
                    WIND_SPEED_KPH, 60,
                    CATEGORY_SAFFIR_SIMPSON, getCycloneCategory(60d)
            )),
            entry("Poly_Orange", Map.of(
                    WIND_SPEED_KPH, 90,
                    CATEGORY_SAFFIR_SIMPSON, getCycloneCategory(90d)
            )),
            entry("Poly_Red", Map.of(
                    WIND_SPEED_KPH, 120,
                    CATEGORY_SAFFIR_SIMPSON, getCycloneCategory(120d)
            ))
    );


    public Map<String, Object> convertProperties(String eventClass, OffsetDateTime polygonDate, String polygonLabelStr, Boolean forecast) {
        Map<String, Object> props = new HashMap<>();

        if (eventClass == null) {
            props.put(AREA_TYPE_PROPERTY, UNKNOWN);
            return props;
        }

        String areaType = areaTypeMap.get(eventClass.replaceAll("\\d", ""));
        props.put(AREA_TYPE_PROPERTY, areaType == null ? UNKNOWN : areaType);

        Boolean isObserved = isObservedMap.get(eventClass);
        if (isObserved != null) {
            props.put(IS_OBSERVED_PROPERTY, isObserved);
        }

        Map<String, Object> severityData = severityDataMap.get(eventClass);
        if (severityData != null) {
            props.put(SEVERITY_DATA_PROPERTY, severityData);
        }

        if ("Poly_Cones_6".equals(eventClass) || "Poly_Cones_12".equals(eventClass) || "Poly_Cones_18".equals(eventClass)) {
            int forecastHours = parseInt(getDigits(eventClass));
            props.put(FORECAST_HRS_PROPERTY, forecastHours);
            props.put(TIMESTAMP_PROPERTY, polygonDate.plusHours(forecastHours));
        } else if (eventClass.startsWith("Line_Line_")) {
            props.put(IS_OBSERVED_PROPERTY, forecast == null || !forecast);
        } else if (eventClass.startsWith("Point_Polygon_Point_")) {
            OffsetDateTime polygonLabel = parsePolygonLabel(polygonDate, polygonLabelStr);
            if (polygonLabel.isAfter(polygonDate)) {
                props.put(IS_OBSERVED_PROPERTY, false);
                props.put(FORECAST_HRS_PROPERTY, HOURS.between(polygonDate, polygonLabel));
                props.put(TIMESTAMP_PROPERTY, polygonLabel);
            } else {
                props.put(IS_OBSERVED_PROPERTY, true);
            }
        }
        return props;
    }

    public void migrateProperties(Map<String, Object> oldProps, Map<String, Object> newProps) {
        newProps.put("Class", oldProps.get("Class"));
        newProps.put("eventname", oldProps.get("eventname"));
        newProps.put("country", oldProps.get("country"));
        newProps.put("eventid", oldProps.get("eventid"));
        newProps.put("eventtype", oldProps.get("eventtype"));
        newProps.put("polygonlabel", oldProps.get("polygonlabel"));
        newProps.put("polygondate", oldProps.get("polygondate"));
    }

    private OffsetDateTime parsePolygonLabel(OffsetDateTime polygonDate, String polygonLabelStr) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("dd/MM HH:mm z")
                .parseDefaulting(YEAR, polygonDate.getYear())
                .toFormatter();
        try {
            OffsetDateTime polygonLabel = ZonedDateTime.parse(polygonLabelStr, formatter).toOffsetDateTime();
            if (MONTHS.between(polygonDate, polygonLabel) > 6) {
                polygonLabel = polygonLabel.withYear(polygonDate.getYear() - 1);
            } else if (MONTHS.between(polygonDate, polygonLabel) < -6) {
                polygonLabel = polygonLabel.withYear(polygonDate.getYear() + 1);
            }
            return polygonLabel;
        } catch (Exception e) {
            return polygonDate;
        }
    }
}
