package io.kontur.eventapi.util;

import io.kontur.eventapi.entity.Severity;

import java.util.Map;
import java.util.regex.Pattern;

public class SeverityUtil {
    public final static String WIND_SPEED_KPH = "windSpeedKph";
    public final static String WIND_GUST_KPH = "windGustsKph";
    public final static String CATEGORY_SAFFIR_SIMPSON = "categorySaffirSimpson";
    public final static String MAGNITUDE = "magnitude";
    public final static String DEPTH_KM = "depthKm";
    public final static String BURNED_AREA_KM2 = "burnedAreaKm2";
    public final static String CONTAINED_AREA_PCT = "containedAreaPct";
    public final static String DROUGHT_AREA_KM2 = "droughtAreaKm2";
    public final static String HAIL_SIZE_MM = "hailSizeMm";
    public final static String CAUSE = "cause";
    public final static String FUJITA_SCALE = "fujitaScale";
    public final static String TORNADO_LENGTH_KM = "tornadoLengthKm";
    public final static String TORNADO_WIDTH_M = "tornadoWidthM";
    public final static String PGA40_MASK = "pga40Mask";
    public final static String COVERAGE_PGA_HIGH_RES = "coveragePgaHighRes";

    public final static String CATEGORY_TD = "TD";
    public final static String CATEGORY_TS = "TS";
    public final static String CATEGORY_1 = "1";
    public final static String CATEGORY_2 = "2";
    public final static String CATEGORY_3 = "3";
    public final static String CATEGORY_4 = "4";
    public final static String CATEGORY_5 = "5";

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

    public static Severity calculateSeverity(Double areaSqKm, long durationHours) {
        if (durationHours <= 24) {
            return Severity.MINOR;
        }
        if (areaSqKm == null) {
            return Severity.UNKNOWN;
        }
        if (areaSqKm < 10) {
            return Severity.MINOR;
        } else if (areaSqKm < 50) {
            return Severity.MODERATE;
        } else if (areaSqKm < 100) {
            return Severity.SEVERE;
        }
        return Severity.EXTREME;
    }

    public static String getCycloneCategory(Double windSpeedKph) {
        if (windSpeedKph == null) {
            return null;
        } else if (windSpeedKph <= 62) {
            return CATEGORY_TD;
        } else if (windSpeedKph <= 118) {
            return CATEGORY_TS;
        } else if (windSpeedKph <= 153) {
            return CATEGORY_1;
        } else if (windSpeedKph <= 177) {
            return CATEGORY_2;
        } else if (windSpeedKph <= 208) {
            return CATEGORY_3;
        } else if (windSpeedKph <= 251) {
            return CATEGORY_4;
        } else {
            return CATEGORY_5;
        }
    }
}
