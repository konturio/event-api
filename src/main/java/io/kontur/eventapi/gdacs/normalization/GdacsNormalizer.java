package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;

import java.util.Map;

public abstract class GdacsNormalizer extends Normalizer {

    private static final Map<String, EventType> typeMap = Map.of(
            "Drought", EventType.DROUGHT,
            "Earthquake", EventType.EARTHQUAKE,
            "Flood", EventType.FLOOD,
            "Tropical Cyclone", EventType.CYCLONE,
            "Volcano Eruption", EventType.VOLCANO,
            "Forest Fires", EventType.WILDFIRE,
            "Forest Fires area", EventType.WILDFIRE,
            "WildFire area", EventType.WILDFIRE
    );

    private static final Map<String, EventType> geometryTypeMap = Map.of(
            "DR", EventType.DROUGHT,
            "EQ", EventType.EARTHQUAKE,
            "FL", EventType.FLOOD,
            "TC", EventType.CYCLONE,
            "VO", EventType.VOLCANO,
            "WF", EventType.WILDFIRE
    );

    private static final Map<String, Severity> severityMap = Map.of(
            "Minor", Severity.MINOR,
            "Moderate", Severity.MODERATE,
            "Severe", Severity.SEVERE,
            "Extreme", Severity.EXTREME
    );

    protected Severity defineSeverity(String severity) {
        return severityMap.getOrDefault(severity, Severity.UNKNOWN);

    }

    protected EventType defineType(String event) {
        return typeMap.getOrDefault(event, EventType.OTHER);
    }

    protected EventType defineGeometryType(String eventType) {
        return geometryTypeMap.getOrDefault(eventType, EventType.OTHER);
    }

    protected String composeExternalEventId(String eventType, String eventId) {
        return eventType + "_" + eventId;
    }
}
