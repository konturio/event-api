package io.kontur.eventapi.tornado.service;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TornadoService {

    public static OffsetDateTime parseDateTimeWithPattern(String dateTimeSting, String pattern) {
        return parseDateTimeWithFormatter(dateTimeSting, DateTimeFormatter.ofPattern(pattern));
    }

    public static OffsetDateTime parseDateTimeWithFormatter(String dateTimeString, DateTimeFormatter formatter) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
        return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }

    public static OffsetDateTime parseDateWithFormatter(String dateString, DateTimeFormatter formatter) {
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }

    public static String makeWktLineString(Double startLon, Double startLat, Double endLon, Double endLat) {
        return String.format("LINESTRING(%s %s, %s %s)", startLon, startLat, endLon, endLat);
    }
}