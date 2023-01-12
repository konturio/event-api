package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;

import java.util.Map;

import static io.kontur.eventapi.util.GeometryUtil.*;

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

}
