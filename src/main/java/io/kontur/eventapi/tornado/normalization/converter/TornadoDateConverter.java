package io.kontur.eventapi.tornado.normalization.converter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;

import static io.kontur.eventapi.tornado.job.NoaaTornadoImportJob.TORNADO_NOAA_PROVIDER;
import static io.kontur.eventapi.tornado.job.StaticTornadoImportJob.*;

public class TornadoDateConverter {

    private final static Map<String, DateTimeFormatter> FORMATTERS = Map.of(
            TORNADO_CANADA_GOV_PROVIDER, DateTimeFormatter.BASIC_ISO_DATE,
            TORNADO_AUSTRALIAN_BM_PROVIDER, DateTimeFormatter.BASIC_ISO_DATE,
            TORNADO_OSM_PROVIDER, DateTimeFormatter.ofPattern("d MMMM yyyy"),
            TORNADO_NOAA_PROVIDER, new DateTimeFormatterBuilder().parseCaseInsensitive()
                    .appendPattern("dd-MMM-")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 1949)
                    .appendPattern(" HH:mm:ss")
                    .toFormatter());

    public static OffsetDateTime parseDateTime(String dateTimeString, String provider) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, FORMATTERS.get(provider));
        return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }

    public static OffsetDateTime parseDate(String dateString, String provider) {
        LocalDate localDate = LocalDate.parse(dateString, FORMATTERS.get(provider));
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }
}