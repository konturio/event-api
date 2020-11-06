package io.kontur.eventapi.gdacs.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GdacsDataLakeConverter {

    public DataLake convertGdacs(ParsedAlert alert, String provider){
        return new DataLake(
                UUID.randomUUID(),
                alert.getIdentifier(),
                alert.getDateModified(),
                DateTimeUtil.uniqueOffsetDateTime(),
                provider,
                alert.getData()
        );
    }
}
