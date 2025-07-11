package io.kontur.eventapi.usgs.earthquake.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class UsgsEarthquakeDataLakeConverter {

    public static final String USGS_EARTHQUAKE_PROVIDER = "usgs.earthquake";

    public DataLake convert(String externalId, OffsetDateTime updatedAt, String data) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(USGS_EARTHQUAKE_PROVIDER);
        dataLake.setData(data);
        return dataLake;
    }
}
