package io.kontur.eventapi.util;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DateTimeUtil {

    public static final DateTimeFormatter ZONED_DATETIME_FORMATTER;
    static {
        Map<Long, String> dow = new HashMap<>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        ZONED_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .optionalStart()
                .appendText(DAY_OF_WEEK, dow)
                .appendLiteral(", ")
                .optionalEnd()
                .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, moy)
                .appendLiteral(' ')
                .appendValue(YEAR, 4)  // 2 digit year not handled
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                // difference from RFC_1123_DATE_TIME: optional offset OR zone ID
                .optionalStart()
                .appendZoneText(TextStyle.SHORT)
                .optionalEnd()
                .optionalStart()
                .appendOffset("+HHMM", "GMT")
                .optionalEnd()
                // use the same resolver style and chronology
                .toFormatter().withResolverStyle(ResolverStyle.SMART).withChronology(IsoChronology.INSTANCE);
    }
    private static final AtomicLong LAST_TIME_MS = new AtomicLong();
    private static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static final String UHC_DATETIME_PATTERN = "dd-MM-yyyy'T'HH:mm:ssXXX";

    public static OffsetDateTime uniqueOffsetDateTime() {
        long m = uniqueCurrentTimeMS();
        Instant instant = Instant.ofEpochMilli(m);
        return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static long uniqueCurrentTimeMS() {
        long now = System.currentTimeMillis();
        while (true) {
            long lastTime = LAST_TIME_MS.get();
            if (lastTime >= now)
                now = lastTime + 1;
            if (LAST_TIME_MS.compareAndSet(lastTime, now))
                return now;
        }
    }

    public static OffsetDateTime parseDateTimeFromString(String value) {
        if (StringUtils.isNotBlank(value)) {
            int idx = value.lastIndexOf(":");
            if (idx > 24) {
                value = value.substring(0, idx) + value.substring(idx + 1);
            }
            return OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
        }
        return null;
    }

    public static OffsetDateTime parseDateTimeByPattern(String value, String pattern) {
        if (StringUtils.isNotBlank(value)) {
            if (pattern == null) {
                pattern = DEFAULT_DATETIME_PATTERN;
            }
            return OffsetDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
        }
        return null;
    }

    public static OffsetDateTime parseZonedDateTimeByFormatter(String value, DateTimeFormatter formatter) {
        if (StringUtils.isNotBlank(value) && formatter != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(value, formatter);
            return zonedDateTime.toOffsetDateTime();
        }
        return null;
    }

    public static OffsetDateTime getDateTimeFromMilli(long dateTimeMilli) {
        return Instant.ofEpochMilli(dateTimeMilli).atOffset(ZoneOffset.UTC);
    }
}
