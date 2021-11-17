package io.kontur.eventapi.calfire.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class CalFireDataLakeConverter {
    public final static String CALFIRE_PROVIDER = "wildfire.calfire";

    public DataLake convertEvent(String event, String externalId, OffsetDateTime updatedAt) throws Exception {
        if (updatedAt == null) {
            throw new Exception("Empty datetime field updated_at for " + externalId);
        }
        OffsetDateTime now = DateTimeUtil.uniqueOffsetDateTime();
        DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, now);
        dataLake.setProvider(CALFIRE_PROVIDER);
        dataLake.setData(event);
        return dataLake;
    }
}
