package io.kontur.eventapi.usgs.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class UsgsShakeMapDataLakeConverter {

    public static final String USGS_SHAKEMAP_PROVIDER = "earthquake.shakemap.usgs";

    public DataLake convertDataLake(String externalId, OffsetDateTime updatedAt, String data) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(USGS_SHAKEMAP_PROVIDER);
        dataLake.setData(data);
        return dataLake;
    }
}
