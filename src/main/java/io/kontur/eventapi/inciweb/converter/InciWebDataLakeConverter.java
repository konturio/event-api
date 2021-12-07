package io.kontur.eventapi.inciweb.converter;

import java.util.UUID;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.inciweb.dto.ParsedItem;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

@Component
public class InciWebDataLakeConverter {
    public final static String INCIWEB_PROVIDER = "wildfire.inciweb";

    public DataLake convertEvent(ParsedItem event) {
        DataLake dataLake = new DataLake(UUID.randomUUID(), event.getGuid(), event.getPubDate(),
                DateTimeUtil.uniqueOffsetDateTime());
        dataLake.setProvider(INCIWEB_PROVIDER);
        dataLake.setData(event.getData());
        return dataLake;
    }
}
