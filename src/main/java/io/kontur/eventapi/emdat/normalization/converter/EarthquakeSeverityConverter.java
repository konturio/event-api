package io.kontur.eventapi.emdat.normalization.converter;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EarthquakeSeverityConverter extends EmDatSeverityConverter {

    /**
     * Returns severity according to the magnitude:
     * <li>EXTREME - 7+</li>
     * <li>SEVERE - 6</li>
     * <li>MODERATE - 4-6</li>
     * <li>MINOR - less than 4</li>
     * <li>UNKNOWN - magnitude is not set</li>
     */
    public Severity defineSeverity(Map<String, String> csvData) {
        String magnitudeStr = csvData.get("Dis Mag Value");
        if (isNumeric(magnitudeStr)) {
            int magnitude = Integer.parseInt(magnitudeStr);
            if (magnitude >= 7) {
                return Severity.EXTREME;
            } else if (magnitude == 6) {
                return Severity.SEVERE;
            } else if (magnitude >= 4) {
                return Severity.MODERATE;
            } else {
                return Severity.MINOR;
            }
        }
        return Severity.UNKNOWN;
    }

    @Override
    public boolean isApplicable(EventType type) {
        return type.equals(EventType.EARTHQUAKE);
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
