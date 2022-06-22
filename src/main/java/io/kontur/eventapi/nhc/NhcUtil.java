package io.kontur.eventapi.nhc;

import java.util.List;

public final class NhcUtil {

    public final static String NHC_AT_PROVIDER = "cyclones.nhc-at.noaa";
    public final static String NHC_CP_PROVIDER = "cyclones.nhc-cp.noaa";
    public final static String NHC_EP_PROVIDER = "cyclones.nhc-ep.noaa";

    public final static List<String> NHC_PROVIDERS = List.of(NHC_AT_PROVIDER, NHC_CP_PROVIDER, NHC_EP_PROVIDER);
}
