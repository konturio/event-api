package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;

import java.util.Map;

public abstract class PdcHazardNormalizer extends Normalizer {

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
