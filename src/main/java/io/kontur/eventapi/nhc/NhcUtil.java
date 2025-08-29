package io.kontur.eventapi.nhc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class NhcUtil {

    public static final String NHC_AT_PROVIDER = "cyclones.nhc-at.noaa";
    public static final String NHC_CP_PROVIDER = "cyclones.nhc-cp.noaa";
    public static final String NHC_EP_PROVIDER = "cyclones.nhc-ep.noaa";

    public static final List<String> NHC_PROVIDERS = List.of(NHC_AT_PROVIDER, NHC_CP_PROVIDER, NHC_EP_PROVIDER);

    public static final String MAIN_REGEXP = "(.*?)(TROPICAL DEPRESSION|SUBTROPICAL DEPRESSION|TROPICAL STORM|HURRICANE \\(TYPHOON\\)|HURRICANE|SUBTROPICAL STORM|POST-TROPICAL CYCLONE \\/ REMNANTS|POST-TROPICAL CYCLONE|POTENTIAL TROPICAL CYCLONE|TROPICAL CYCLONE|POTENTIAL TROP CYCLONE|REMNANTS)( OF )?(.*?)(?: SPECIAL)?\\s*(?=(FORECAST\\/ADVISORY))FORECAST\\/ADVISORY NUMBER ([0-9]{1,}).*((AL[0-9]{6})|([EC]P[0-9]{6}))(.*?)?([0-9]{4} \\w{3} \\w{3} \\w{3} [0-9]{2} [0-9]{4})(.*)?(TROPICAL DEPRESSION|SUBTROPICAL DEPRESSION|TROPICAL STORM|HURRICANE \\(TYPHOON\\)|HURRICANE|SUBTROPICAL STORM|POST-TROPICAL CYCLONE \\/ REMNANTS|POST-TROPICAL CYCLONE|POTENTIAL TROPICAL CYCLONE|TROPICAL CYCLONE|POTENTIAL TROP CYCLONE|REMNANTS)( OF )?(.*)?(CENTER LOCATED NEAR .*)(MAX SUSTAINED WINDS.*)(REPEAT.*?)( FORECAST VALID .*?)?( EXTENDED OUTLOOK.*?)?( OUTLOOK VALID .*?)?( REQUEST.*)?$";

    public static final String CENTER_REGEXP = "CENTER LOCATED NEAR\\s*([0-9]*\\.[0-9]*[NS])\\s*([0-9]*\\.[0-9]*[WE]) AT ([0-9]{2})\\/([0-9]{2})([0-9]{2})Z";

    public static final String MAX_SUSTAINED_WIND_REGEXP = "MAX SUSTAINED WINDS\\s*([0-9]*) KT WITH GUSTS TO\\s*([0-9]*) KT\\.(\\s*64 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*50 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*34 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*12 FT SEAS\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?";

    public static final String FORECAST_REGEXP = "(FORECAST VALID\\s*[0-9]{2}\\/[0-9]{4}Z\\s*[0-9\\.]{1,}[N|S]\\s*[0-9\\.]{1,}[W|E]?.*?MAX WIND\\s*[0-9\\.]{1,} KT.*?GUSTS\\s*[0-9\\.]{1,} KT\\.(\\s*64 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*50 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*34 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*12 FT SEAS\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?)";

    public static final String OUTLOOK_REGEXP = "(OUTLOOK VALID\\s*[0-9]{2}\\/[0-9]{4}Z\\s*[0-9\\.]{1,}[N|S]\\s*[0-9\\.]{1,}[W|E].*?MAX WIND.*?[0-9\\.]{1,} KT.*?GUSTS\\s*[0-9\\.]{1,} KT\\.)";

    public static final String WIND_SPEED_REGEXP = "([0-9]{2})\\/([0-9]{2})([0-9]{2})Z\\s*([0-9\\.]{1,}[N|S])?\\s*([0-9\\.]{1,}[W|E])?.*?MAX WIND\\s*([0-9\\.]{1,})? KT.*?GUSTS\\s*([0-9\\.]{1,}) KT\\.(\\s*64 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*50 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*34 KT\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?(\\s*12 FT SEAS\\.*\\s*[0-9]*NE\\s*[0-9]*SE\\s*[0-9]*SW\\s*[0-9]*NW\\.)?";

    public static final String WIND_SECTIONS_REGEXP = "(64|50|34) KT\\.*\\s*([0-9]*)NE\\s*([0-9]*)SE\\s*([0-9]*)SW\\s*([0-9]*)NW";

    public static final int SEVERITY_MINOR_MAX_WIND_SPEED = 33;
    public static final int SEVERITY_MODERATE_MAX_WIND_SPEED = 63;
    public static final int SEVERITY_SEVERE_MAX_WIND_SPEED = 82;
    public static final Integer TYPE_POS = 2;
    public static final Integer NAME_POS = 4;
    public static final Integer ADV_NUMBER_POS = 6;
    public static final Integer EVENT_ID_POS = 7;
    public static final Integer CURRENT_TIME_POS = 11;
    public static final Integer NEWS_POS = 12;
    public static final Integer CENTER_POS = 16;
    public static final Integer MAX_SUSTAINED_WIND_POS = 17;
    public static final Integer FORECAST_POS = 19;
    public static final Integer OUTLOOK_POS = 21;

    // MAX SUSTAINED WIND
    public static final Integer MAX_WIND_POS = 1;
    public static final Integer MAX_GUSTS_POS = 2;
    public static final Integer MAX_WIND_64_POS = 3;
    public static final Integer MAX_WIND_34_POS = 5;


    // CENTER LOCATION
    public static final Integer CENTER_LAT_POS = 1;
    public static final Integer CENTER_LONG_POS = 2;
    public static final Integer CENTER_DAY_POS = 3;
    public static final Integer CENTER_HOURS_POS = 4;
    public static final Integer CENTER_MINUTES_POS = 5;

    // FORECASTS
    public static final Integer FORECAST_DAY_POS = 1;
    public static final Integer FORECAST_HOURS_POS = 2;
    public static final Integer FORECAST_MINUTES_POS = 3;
    public static final Integer FORECAST_LAT_POS = 4;
    public static final Integer FORECAST_LONG_POS = 5;
    public static final Integer FORECAST_WIND_POS = 6;
    public static final Integer FORECAST_GUSTS_POS = 7;
    public static final Integer FORECAST_64_POS = 8;
    public static final Integer FORECAST_34_POS = 10;
    public static final Integer WIND_PROP_NAME_POS = 1;
    public static final Integer WIND_NE_POS = 2;
    public static final Integer WIND_SE_POS = 3;
    public static final Integer WIND_SW_POS = 4;
    public static final Integer WIND_NW_POS = 5;

    public static final Double COEFFICIENT_KNOTS_TO_KPH = 1.852d;

    public static Double convertKnotsToKph(Double speedInKnots, int scale) {
        return BigDecimal.valueOf(COEFFICIENT_KNOTS_TO_KPH * speedInKnots)
                .setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static Integer convertKnotsToKph(Double speedInKnots) {
        return BigDecimal.valueOf(COEFFICIENT_KNOTS_TO_KPH * speedInKnots)
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }

}
