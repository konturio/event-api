package io.kontur.eventapi.nifc.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class NifcDataLakeConverter {
    public static final String NIFC_PERIMETERS_PROVIDER = "wildfire.perimeters.nifc";
    public static final String NIFC_LOCATIONS_PROVIDER = "wildfire.locations.nifc";

    public DataLake convertDataLake(String externalId, OffsetDateTime updatedAt, String provider, String data) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(provider);
        dataLake.setData(data);
        return dataLake;
    }
}
