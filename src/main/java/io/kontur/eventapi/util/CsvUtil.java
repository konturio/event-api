package io.kontur.eventapi.util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CsvUtil {

    private static final String CSV_SEPARATOR = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; // splits on comma outside the double quotes - https://stackoverflow.com/questions/18893390/splitting-on-comma-outside-quotes

    public static Map<String, String> parseRow(String csvHeader, String csvRow) {
        String[] csvRows = csvRow.split(CSV_SEPARATOR);
        String[] csvHeaders = csvHeader.split(CSV_SEPARATOR);

        return IntStream.range(0, csvHeaders.length).boxed()
                .collect(Collectors.toMap(i -> csvHeaders[i], i -> {
                    if (i >= csvRows.length) {
                        return "";
                    }
                    return trimDoubleQuotes(csvRows[i]);
                }));
    }

    private static String trimDoubleQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
