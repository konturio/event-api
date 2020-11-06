package io.kontur.eventapi.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class DateTimeUtil {

    private static final AtomicLong LAST_TIME_MS = new AtomicLong();

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

    public static OffsetDateTime parseDateTimeFromString(String value){
        return OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
