package io.kontur.eventapi.gdacs.util;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class GdacsDescriptionCleaner {

    private static final Pattern DATE_PREFIX = Pattern.compile("^(On \\d{1,2}/\\d{1,2}/\\d{4}( \\d{1,2}:\\d{2}(?::\\d{2})? (?:AM|PM))?,?[^.]*\\.\\s*)");
    private static final Pattern PERIOD_RANGE_PREFIX = Pattern.compile("^From \\d{1,2}/\\d{1,2}/\\d{4} to \\d{1,2}/\\d{1,2}/\\d{4},\\s*");
    private static final Pattern FOREST_FIRE_SENTENCE = Pattern.compile("^On \\d{1,2}/\\d{1,2}/\\d{4}, a forest fire started.*?\\.\\s*$");

    private GdacsDescriptionCleaner() {
    }

    public static String clean(String description) {
        if (description == null) {
            return null;
        }
        String result = description.trim();

        // Remove drought alert statements regardless of specified level
        result = result.replaceAll("^The\\s+Drought alert level is [^.]*\\.\\s*", "");

        result = DATE_PREFIX.matcher(result).replaceFirst("");
        result = PERIOD_RANGE_PREFIX.matcher(result).replaceFirst("");

        if (FOREST_FIRE_SENTENCE.matcher(result).matches()) {
            return "";
        }

        result = result.replaceAll("\\(vulnerability \\[unknown\\])", "");
        result = result.replaceAll("Estimated population affected by category 1 \(120 km/h\) wind speeds or higher is 0\\s*(\\(0 in tropical storm\\))?\\.?", "");
        result = result.replaceAll("The flood caused 0 deaths and 0 displaced \\.", "");
        result = result.replaceAll("The flood caused 0 deaths and (\\d+) displaced", "The flood caused $1 displaced");
        result = result.replaceAll("The flood caused (\\d+) deaths and 0 displaced", "The flood caused $1 deaths");
        result = result.replaceAll("potentially affecting No people affected[^.]*\\.", "");

        result = StringUtils.normalizeSpace(result);
        result = RegExUtils.replacePattern(result, " \\.", ".");
        return result.trim();
    }
}
