package io.kontur.eventapi.jtwc;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class JtwcUtil {

    public static final String JTWC_PROVIDER = "cyclones.jtwc.mil";

    public static final double COEFFICIENT_KNOTS_TO_KPH = 1.852d;

    public static Double convertKnotsToKph(Double speedInKnots, int scale) {
        return BigDecimal.valueOf(COEFFICIENT_KNOTS_TO_KPH * speedInKnots)
                .setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static Integer convertKnotsToKph(Double speedInKnots) {
        return BigDecimal.valueOf(COEFFICIENT_KNOTS_TO_KPH * speedInKnots)
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
