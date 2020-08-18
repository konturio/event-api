package io.kontur.eventapi.normalization;

import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedObservationsDto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public abstract class Normalizer {

    public abstract boolean isApplicable(EventDataLakeDto dataLakeDto);

    public abstract NormalizedObservationsDto normalize(EventDataLakeDto dataLakeDto);

    protected String readString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    protected Boolean readBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : Boolean.valueOf(readString(map, key));
    }

    protected Long readLong(Map<String, Object> map, String key) {
        String value = readString(map, key);
        return value == null ? null : Long.valueOf(value);
    }

    protected Integer readInt(Map<String, Object> map, String key) {
        String value = readString(map, key);
        return value == null ? null : Integer.valueOf(value);
    }

    protected Double readDouble(Map<String, Object> map, String key) {
        String value = readString(map, key);
        return value == null ? null : Double.valueOf(value);
    }

    protected OffsetDateTime readDateTime(Map<String, Object> map, String key) {
        Long value = readLong(map, key);
        return value == null ? null : OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC);
    }

    protected String makeWktPoint(Double lon, Double lat) {
        return String.format("POINT(%s %s)", lon, lat);
    }
}
