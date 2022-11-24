package io.kontur.eventapi.inciweb;

public final class InciWebUtil {

    public static final String COORDINATES_REGEXP = "Latitude: ([0-9-]{1,})?(°)?([0-9]{1,})?(')?([0-9]{1,})?('')?.*Longitude: ([0-9-]{1,})?(°)?([0-9]{1,})?(')?([0-9]{1,})?('')?";
    public static final Integer LAT_DEGREE = 1;
    public static final Integer LAT_MINUTES = 3;
    public static final Integer LAT_SECONDS = 5;
    public static final Integer LON_DEGREE = 7;
    public static final Integer LON_MINUTES = 9;
    public static final Integer LON_SECONDS = 11;

}
