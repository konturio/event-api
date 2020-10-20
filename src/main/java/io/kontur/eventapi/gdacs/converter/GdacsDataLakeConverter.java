package io.kontur.eventapi.gdacs.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GdacsDataLakeConverter {

    public final static String GDACS_PROVIDER = "gdacs";

    public DataLake convertGdacs(AlertForInsertDataLake alert){
        return new DataLake(
                UUID.randomUUID(),
                alert.getExternalId(),
                alert.getUpdateDate(),
                DateTimeUtil.uniqueOffsetDateTime(),
                GDACS_PROVIDER,
                alert.getData()
        );
    }
}
