package io.kontur.eventapi.gdacs.converter;

import io.kontur.eventapi.cap.converter.CapDataLakeConverter;
import io.kontur.eventapi.cap.dto.CapParsedItem;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GdacsDataLakeConverter implements CapDataLakeConverter {

    public final static String GDACS_ALERT_PROVIDER = "gdacsAlert";
    public final static String GDACS_ALERT_GEOMETRY_PROVIDER = "gdacsAlertGeometry";

    public DataLake convertGdacs(ParsedAlert alert) {
        var dataLake = convertCommonData(alert);
        dataLake.setProvider(GDACS_ALERT_PROVIDER);
        dataLake.setData(alert.getData());
        return dataLake;
    }

    public DataLake convertGdacsWithGeometry(ParsedAlert alert, String geometry) {
        var dataLake = convertCommonData(alert);
        dataLake.setProvider(GDACS_ALERT_GEOMETRY_PROVIDER);
        dataLake.setData(geometry);
        return dataLake;
    }

    private DataLake convertCommonData(ParsedAlert alert) {
        return new DataLake(
                UUID.randomUUID(),
                alert.getIdentifier(),
                alert.getDateModified(),
                DateTimeUtil.uniqueOffsetDateTime()
        );
    }

    @Override
    public DataLake convertEvent(CapParsedItem event, String provider) {
        return null;
    }
}
