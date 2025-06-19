package io.kontur.eventapi.util;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.kontur.eventapi.firms.dto.ParsedDataLakeItem;
import io.kontur.eventapi.firms.dto.ParsedNormalizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CsvUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CsvUtil.class);

    private static final String CSV_SEPARATOR = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; // splits on comma outside the double quotes - https://stackoverflow.com/questions/18893390/splitting-on-comma-outside-quotes

    private static final CsvMapper csvMapper = new CsvMapper();
    private static final CsvSchema schema = CsvSchema.builder()
            .setLineSeparator("\n")
            .setColumnSeparator(',')
            .build()
            .withHeader();

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

    public static ParsedNormalizationItem parseNormalizationRow(String provider, String csvHeader, String csvRow) {
        try {
            final MappingIterator<ParsedNormalizationItem> mappingIterator = csvMapper
                    .readerFor(ParsedNormalizationItem.class).with(schema)
                    .readValues(csvHeader + "\n" + csvRow);
            return mappingIterator.hasNext() ? mappingIterator.next() : null;
        } catch (IOException e) {
            LOG.error("Error while parsing csv for {}. {}", provider, e.getMessage());
        }
        return null;
    }

    public static ParsedDataLakeItem parseDataLakeRow(String provider, String csvHeader, String csvRow) {
        try {
            final MappingIterator<ParsedDataLakeItem> mappingIterator = csvMapper
                    .readerFor(ParsedDataLakeItem.class).with(schema)
                    .readValues(csvHeader + "\n" + csvRow);
            return mappingIterator.hasNext() ? mappingIterator.next() : null;
        } catch (IOException e) {
            LOG.error("Error while parsing csv for {}. {}", provider, e.getMessage());
        }
        return null;
    }
}
