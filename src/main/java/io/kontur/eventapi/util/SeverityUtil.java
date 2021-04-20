package io.kontur.eventapi.util;

import io.kontur.eventapi.entity.Severity;

import java.util.Map;
import java.util.regex.Pattern;

public class SeverityUtil {
    private final static Pattern fujitaPattern = Pattern.compile("(F|EF)?[0-5]");

    private final static Map<String, Severity> fujitaMapper = Map.of(
            "0", Severity.MINOR,
            "1", Severity.MODERATE,
            "2", Severity.MODERATE,
            "3", Severity.SEVERE,
            "4", Severity.EXTREME,
            "5", Severity.EXTREME);

    public static Severity convertFujitaScale(String fujita) {
        return fujita != null && fujitaPattern.matcher(fujita).matches()
                ? fujitaMapper.get(fujita.substring(fujita.length() - 1))
                : Severity.UNKNOWN;
    }
}
