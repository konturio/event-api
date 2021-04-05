package io.kontur.eventapi.util;

import io.kontur.eventapi.entity.Severity;

import java.util.Map;

public class SeverityUtil {
    private final static Map<String, Severity> FUJITA_SCALE_MAP = Map.of(
            "0", Severity.MINOR,
            "1", Severity.MODERATE,
            "2", Severity.MODERATE,
            "3", Severity.SEVERE,
            "4", Severity.EXTREME,
            "5", Severity.EXTREME
    );

    public static Severity convertFujitaScale(String fujita) {
        return fujita == null ? Severity.UNKNOWN : FUJITA_SCALE_MAP.getOrDefault(fujita, Severity.UNKNOWN);
    }
}
