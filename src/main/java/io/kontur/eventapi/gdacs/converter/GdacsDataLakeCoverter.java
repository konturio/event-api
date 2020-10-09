package io.kontur.eventapi.gdacs.converter;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class GdacsDataLakeCoverter {

    public final static String GDACS_PROVIDER = "gdacs";

    public DataLake covertGdacs(AlertForInsertDataLake alert){
        return new DataLake(
                UUID.randomUUID(),
                alert.getExternalId(),
                alert.getUpdateDate(),
                OffsetDateTime.now(ZoneOffset.UTC),
                GDACS_PROVIDER,
                alert.getData()
        );
    }
}
