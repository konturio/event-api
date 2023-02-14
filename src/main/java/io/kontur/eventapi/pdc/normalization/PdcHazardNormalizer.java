package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.kontur.eventapi.util.GeometryUtil.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public abstract class PdcHazardNormalizer extends Normalizer {

    protected static final Map<String, Object> EXPOSURE_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, EXPOSURE, IS_OBSERVED_PROPERTY, true);
    protected final static Map<String, Object> HAZARD_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, CENTER_POINT);
    protected final static Map<String, Object> MAG_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, ALERT_AREA, IS_OBSERVED_PROPERTY, true);
    protected final static Map<String, Object> SQS_CYCLONE_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, POSITION, IS_OBSERVED_PROPERTY, true);
    protected final static String ORIGIN_NASA = "NASA";

    private static final Map<String, EventType> typeMap = Map.of(
            "FLOOD", EventType.FLOOD,
            "TSUNAMI", EventType.TSUNAMI,
            "TORNADO", EventType.TORNADO,
            "WILDFIRE", EventType.WILDFIRE,
            "WINTERSTORM", EventType.WINTER_STORM,
            "EARTHQUAKE", EventType.EARTHQUAKE,
            "STORM", EventType.STORM,
            "CYCLONE", EventType.CYCLONE,
            "DROUGHT", EventType.DROUGHT,
            "VOLCANO", EventType.VOLCANO
    );

    private static final Map<String, Severity> severityMap = Map.of(
            "WARNING", Severity.EXTREME,
            "WATCH", Severity.SEVERE,
            "ADVISORY", Severity.MODERATE,
            "INFORMATION", Severity.MINOR,
            "TERMINATION", Severity.TERMINATION
    );

    protected Severity defineSeverity(String severityId) {
        return severityMap.getOrDefault(severityId, Severity.UNKNOWN);
    }

    protected EventType defineType(String typeId) {
        return typeMap.getOrDefault(typeId, EventType.OTHER);
    }

    protected Boolean defineActive(String status) {
        return equalsIgnoreCase(status, "A") ? TRUE : equalsIgnoreCase(status, "E") ? FALSE : null;
    }

    protected Boolean defineAutoExpire(String autoExpire) {
        return equalsIgnoreCase(autoExpire, "Y") ? TRUE : equalsIgnoreCase(autoExpire, "N") ? FALSE : null;
    }

    protected String defineOrigin(String description) {
        if (contains(description, "NASA")) return ORIGIN_NASA;
        return null;
    }

    protected BigDecimal parseRebuildCost(String description) {
        if (description != null) {
            if (contains(description, "no major population centers are within the affected area")) {
                return BigDecimal.ZERO;
            }
            Matcher matcher = Pattern.compile("\\$([\\d.,]+)\\s(Million|Billion|Trillion)?\\s?of infrastructure").matcher(description);
            if (matcher.find()) {
                BigDecimal loss = new BigDecimal(matcher.group(1).replace(",", ""));
                if (matcher.group(2) == null) return loss;
                switch (matcher.group(2)) {
                    case "Million": return loss.multiply(BigDecimal.valueOf(1_000_000.));
                    case "Billion": return loss.multiply(BigDecimal.valueOf(1_000_000_000.));
                    case "Trillion": return loss.multiply(BigDecimal.valueOf(1_000_000_000_000.));
                }
            }
        }
        return null;
    }

    protected String parseLocation(String name) {
        if (name != null) {
            Matcher matcher = Pattern.compile("(\\w+)\\s-\\s(.+)").matcher(name);
            if (matcher.find()) return matcher.group(2);
        }
        return null;
    }
}
