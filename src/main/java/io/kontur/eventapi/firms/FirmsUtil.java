package io.kontur.eventapi.firms;

import java.util.List;

public final class FirmsUtil {

    public final static String MODIS_PROVIDER = "firms.modis-c6";
    public final static String SUOMI_PROVIDER = "firms.suomi-npp-viirs-c2";
    public final static String NOAA_PROVIDER = "firms.noaa-20-viirs-c2";

    public final static List<String> FIRMS_PROVIDERS = List.of(MODIS_PROVIDER, SUOMI_PROVIDER, NOAA_PROVIDER);
}
