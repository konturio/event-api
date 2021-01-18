package io.kontur.eventapi.emdat.normalization.converter;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CycloneSeverityConverter extends EmDatSeverityConverter {

    /**
     * Returns severity according to the wind speed:
     * <li>EXTREME - 209+ kph</li>
     * <li>SEVERE - 154 - 208 kph</li>
     * <li>MODERATE - less than 154 kph</li>
     * <li>UNKNOWN - wind speed is not set</li>
     */
    public Severity defineSeverity(Map<String, String> csvData) {
        String windSpeedStr = csvData.get("Dis Mag Value");
        if (isNumeric(windSpeedStr)) {
            int windSpeed = Integer.parseInt(windSpeedStr);
            if (windSpeed >= 209) {
                return Severity.EXTREME;
            } else if (windSpeed >= 154) {
                return Severity.SEVERE;
            } else {
                return Severity.MODERATE;
            }
        }
        return Severity.UNKNOWN;
    }

    @Override
    public boolean isApplicable(EventType type) {
        return type.equals(EventType.TORNADO) || type.equals(EventType.CYCLONE);
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
