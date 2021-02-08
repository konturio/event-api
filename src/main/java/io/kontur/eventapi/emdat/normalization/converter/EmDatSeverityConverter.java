package io.kontur.eventapi.emdat.normalization.converter;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.job.Applicable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmDatSeverityConverter implements Applicable<EventType> {

    /**
     * Returns severity according to the next rules:
     * <li>EXTREME - 10 and more deaths</li>
     * <li>SEVERE - Population was affected or less than 10 deaths</li>
     * <li>MODERATE - Population was not affected but there was some $ damage</li>
     * <li>UNKNOWN - Population was not affected and no $ damage</li>
     */
    public Severity defineSeverity(Map<String, String> csvData) {
        String deaths = csvData.get("Total Deaths");
        if (isNumeric(deaths)) {
            if (Integer.parseInt(deaths) >= 10) {
                return Severity.EXTREME;
            } else {
                return Severity.SEVERE;
            }
        }
        String affected = csvData.get("Total Affected");
        if (isNumeric(affected)) {
            return Severity.SEVERE;
        }
        String damage = csvData.get("Total Damages ('000 US$)");
        if (isNumeric(damage)) {
            return Severity.MODERATE;
        }
        return Severity.UNKNOWN;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    protected boolean isNumeric(String strNum) {
        if (strNum == null || strNum.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
