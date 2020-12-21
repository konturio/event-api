package io.kontur.eventapi.resource.dto;

import java.time.OffsetDateTime;

public class DateTimeRange {

    private OffsetDateTime from;
    private OffsetDateTime to;

    public DateTimeRange(String val) {
        if (val.contains("/")) {
            final String[] split = val.split("/");
            if (!split[0].isEmpty() && !split[0].equals("..")) {
                from = OffsetDateTime.parse(split[0]);
            }
            if (!split[1].isEmpty() && !split[1].equals("..")) {
                to = OffsetDateTime.parse(split[1]);
            }
            if (from == null && to == null) {
                throw new IllegalArgumentException("interval should not be opened from both ends");
            }
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("range lower bound must be less than or equal to range upper bound");
            }
        } else {
            final OffsetDateTime dt = OffsetDateTime.parse(val);
            from = dt;
            to = dt;
        }
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public OffsetDateTime getTo() {
        return to;
    }
}
