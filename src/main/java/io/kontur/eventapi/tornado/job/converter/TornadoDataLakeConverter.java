package io.kontur.eventapi.tornado.job.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class TornadoDataLakeConverter {

    public DataLake convertDataLake(String externalId, OffsetDateTime updatedAt, String provider, String data) {
        DataLake dataLake = new DataLake();
        dataLake.setObservationId(UUID.randomUUID());
        dataLake.setExternalId(externalId);
        dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setUpdatedAt(updatedAt);
        dataLake.setProvider(provider);
        dataLake.setData(data);
        return dataLake;
    }
}
