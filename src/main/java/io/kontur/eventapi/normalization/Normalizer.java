package io.kontur.eventapi.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.job.Applicable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public abstract class Normalizer implements Applicable<DataLake> {

    public abstract NormalizedObservation normalize(DataLake dataLakeDto);

    public boolean isSkipped() {
        return false;
    }

    protected final static ObjectMapper mapper = new ObjectMapper();

    public static String readString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    protected Boolean readBoolean(Map<String, Object> map, String key) {
        String value = readString(map, key);
        return value == null ? null : Boolean.valueOf(value);
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

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readMap(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value == null ? null : mapper.convertValue(value, Map.class);
    }

    protected String makeWktPoint(Double lon, Double lat) {
        return lon == null || lat == null ? null : String.format("POINT(%s %s)", lon, lat);
    }

    protected String makeWktLine(Double lon1, Double lat1, Double lon2, Double lat2) {
        return lon1 == null || lat1 == null || lon2 == null || lat2 == null
                ? null
                : String.format("LINESTRING(%s %s, %s %s)", lon1, lat1, lon2, lat2);
    }
}
