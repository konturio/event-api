package io.kontur.eventapi.swissre.util;

import io.kontur.eventapi.entity.Severity;

import java.util.Map;

public class StaticTornadoUtil {
    public final static String CANADA_GOV = "canada-gov";
    public final static String AUSTRALIA_BM = "australia_bm";

    public final static Map<String, String> PROVIDERS = Map.of(
            CANADA_GOV, "static/kontur_tornado_pt_canada.json",
            AUSTRALIA_BM, "static/kontur_tornado_pt_australia.json"
    );

    public final static Map<String, String> COUNTRY_NAMES = Map.of(
            CANADA_GOV, "Canada",
            AUSTRALIA_BM, "Australia"
    );

    public final static Map<String, Severity> SEVERITIES = Map.of(
            "0", Severity.MINOR,
            "1", Severity.MODERATE,
            "2", Severity.MODERATE,
            "3", Severity.SEVERE,
            "4", Severity.EXTREME,
            "5", Severity.EXTREME
    );

    public final static Map<String, String> SOURCE_UPDATES = Map.of(
            CANADA_GOV, "20180816",
            AUSTRALIA_BM, "20200101"
    );

    public final static String UNKNOWN = "Unknown";
}
