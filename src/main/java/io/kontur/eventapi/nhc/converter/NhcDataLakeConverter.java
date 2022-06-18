package io.kontur.eventapi.nhc.converter;

import java.util.UUID;

import io.kontur.eventapi.converter.DataLakeConverter;
import io.kontur.eventapi.dto.ParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

@Component
public class NhcDataLakeConverter implements DataLakeConverter {

    public DataLake convertEvent(ParsedItem event, String provider) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), event.getGuid(), event.getPubDate(),
                DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(provider);
        dataLake.setData(event.getData());
        return dataLake;
    }
}
