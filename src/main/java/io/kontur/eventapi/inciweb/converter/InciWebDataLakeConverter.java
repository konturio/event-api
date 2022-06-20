package io.kontur.eventapi.inciweb.converter;

import java.util.UUID;

import io.kontur.eventapi.cap.converter.CapDataLakeConverter;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

@Component
public class InciWebDataLakeConverter implements CapDataLakeConverter {

    public DataLake convertEvent(CapParsedItem event, String provider) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), event.getGuid(), event.getPubDate(),
                DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(provider);
        dataLake.setData(event.getData());
        return dataLake;
    }
}
