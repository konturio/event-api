package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;

import java.math.BigDecimal;
import java.math.BigInteger;
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
    public final static String ORIGIN_NASA = "NASA";

    private static final Map<String, EventType> typeMap = Map.ofEntries(
            Map.entry("FLOOD", EventType.FLOOD),
            Map.entry("TSUNAMI", EventType.TSUNAMI),
            Map.entry("TORNADO", EventType.TORNADO),
            Map.entry("WILDFIRE", EventType.WILDFIRE),
            Map.entry("WINTERSTORM", EventType.WINTER_STORM),
            Map.entry("EARTHQUAKE", EventType.EARTHQUAKE),
            Map.entry("STORM", EventType.STORM),
            Map.entry("CYCLONE", EventType.CYCLONE),
            Map.entry("DROUGHT", EventType.DROUGHT),
            Map.entry("VOLCANO", EventType.VOLCANO),
            Map.entry("HIGHWIND", EventType.STORM),
            Map.entry("COMBAT", EventType.SITUATION),
            Map.entry("TERRORISM", EventType.SITUATION),
            Map.entry("CIVILUNREST", EventType.SITUATION),
            Map.entry("ACTIVESHOOTER", EventType.SITUATION),
            Map.entry("POLITICALCONFLICT", EventType.SITUATION),
            Map.entry("CYBER", EventType.SITUATION)
    );

    private static final Map<String, Severity> severityMap = Map.of(
            "WARNING", Severity.EXTREME,
            "WATCH", Severity.SEVERE,
            "ADVISORY", Severity.MODERATE,
            "INFORMATION", Severity.MINOR,
            "TERMINATION", Severity.TERMINATION
    );

    public static Severity defineSeverity(String severityId) {
        return severityMap.getOrDefault(severityId, Severity.UNKNOWN);
    }

    public static EventType defineType(String typeId) {
        return typeMap.getOrDefault(typeId, EventType.OTHER);
    }

    public static Boolean defineActive(String status) {
        return equalsIgnoreCase(status, "A") ? TRUE : equalsIgnoreCase(status, "E") ? FALSE : null;
    }

    public static Boolean defineAutoExpire(String autoExpire) {
        return equalsIgnoreCase(autoExpire, "Y") ? TRUE : equalsIgnoreCase(autoExpire, "N") ? FALSE : null;
    }

    public static String defineOrigin(String description) {
        if (contains(description, "NASA")) return ORIGIN_NASA;
        return null;
    }

    public static BigInteger parseRebuildCost(String description) {
        if (description != null) {
            if (contains(description, "no major population centers are within the affected area")) {
                return BigInteger.ZERO;
            }
            Matcher matcher = Pattern.compile("\\$([\\d.,]+)\\s(Million|Billion|Trillion)?\\s?of infrastructure").matcher(description);
            if (matcher.find()) {
                BigDecimal loss = new BigDecimal(matcher.group(1).replace(",", ""));
                if (matcher.group(2) == null) return loss.toBigInteger();
                switch (matcher.group(2)) {
                    case "Million": return loss.multiply(BigDecimal.valueOf(1_000_000.)).toBigInteger();
                    case "Billion": return loss.multiply(BigDecimal.valueOf(1_000_000_000.)).toBigInteger();
                    case "Trillion": return loss.multiply(BigDecimal.valueOf(1_000_000_000_000.)).toBigInteger();
                }
            }
        }
        return null;
    }

    public static String parseLocation(String name) {
        if (name != null) {
            Matcher matcher = Pattern.compile("(\\w+)\\s-\\s(.+)").matcher(name);
            if (matcher.find()) return matcher.group(2);
        }
        return null;
    }
}
