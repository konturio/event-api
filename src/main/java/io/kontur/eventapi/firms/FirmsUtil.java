package io.kontur.eventapi.firms;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class FirmsUtil {
    public final static String MODIS_PROVIDER = "firms.modis-c6";
    public final static String SUOMI_PROVIDER = "firms.suomi-npp-viirs-c2";
    public final static String NOAA_PROVIDER = "firms.noaa-20-viirs-c2";

    private static final String CSV_SEPARATOR = ",";

    public static Map<String, String> parseRow(String csvHeader, String csvRow) {
        String[] csvRows = csvRow.split(CSV_SEPARATOR);
        String[] csvHeaders = csvHeader.split(CSV_SEPARATOR);

        return IntStream.range(0, csvHeaders.length).boxed()
                .collect(Collectors.toMap(i -> csvHeaders[i], i -> csvRows[i]));
    }
}
