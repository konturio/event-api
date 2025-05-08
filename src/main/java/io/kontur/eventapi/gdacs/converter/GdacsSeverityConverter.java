package io.kontur.eventapi.gdacs.converter;

import io.kontur.eventapi.entity.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.kontur.eventapi.entity.EventType.*;
import static io.kontur.eventapi.util.SeverityUtil.*;

@Component
public class GdacsSeverityConverter {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSeverityConverter.class);

    private final Map<EventType, String> typeSeverityUnit = Map.of(
            CYCLONE, "km/h",
            EARTHQUAKE, "M",
            WILDFIRE, "ha",
            DROUGHT, "km2"
    );

    private final Map<EventType, String> typeSeverityName = Map.of(
            CYCLONE, WIND_SPEED_KPH,
            EARTHQUAKE, MAGNITUDE,
            WILDFIRE, BURNED_AREA_KM2,
            DROUGHT, DROUGHT_AREA_KM2
    );

    Pattern depthPattern = Pattern.compile("Depth:(\\d+(\\.\\d+)?)km");

    public Map<String, Object> getSeverityData(Double severity, String severityUnit, String severityText, EventType type) {
        Map<String, Object> severityData = new HashMap<>();
        try {
            if (typeSeverityUnit.containsKey(type)) {
                if (typeSeverityUnit.get(type).equals(severityUnit)) {
                    severityData.put(typeSeverityName.get(type), severity);
                } else {
                    LOG.error("Unknown GDACS severity unit for {}: {}", type, severityUnit);
                }
            }
            if (EARTHQUAKE.equals(type)) {
                Matcher matcher = depthPattern.matcher(severityText);
                if (matcher.find()) {
                    severityData.put(DEPTH_KM, Double.parseDouble(matcher.group(1)));
                }
            }
            if (CYCLONE.equals(type)) {
                severityData.put(CATEGORY_SAFFIR_SIMPSON, getCycloneCategory(severity));
            }
        } catch (Exception e) {
            LOG.error("Failed to process GDACS severity, severity: {}, severity unit: {}, severity text: {}",
                    severity, severityUnit, severityText, e);
        }

        return severityData;
    }
}
