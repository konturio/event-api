package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Component;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;

@Component
public class GdacsGeometryNormalizer extends GdacsNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return GDACS_ALERT_GEOMETRY_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setExternalEpisodeId(dataLakeDto.getExternalId());
        normalizedObservation.setActive(true);

        FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(dataLakeDto.getData());
        Map<String, Object> properties = featureCollection.getFeatures()[0].getProperties();

        normalizedObservation.setGeometries(featureCollection);

        String eventType = readString(properties, "eventtype");
        String eventId = readString(properties, "eventid");
        normalizedObservation.setType(defineGeometryType(eventType));
        normalizedObservation.setExternalEventId(composeExternalEventId(eventType, eventId));

        normalizedObservation.setProperName(readString(properties, "eventname").trim());
        normalizedObservation.setStartedAt(parseDateTime(readString(properties, "fromdate")));
        normalizedObservation.setEndedAt(parseDateTime(readString(properties, "todate")));

        Map<String, Object> urlMap = readMap(properties, "url");
        normalizedObservation.setSourceUri(urlMap == null ? null : readString(urlMap, "report"));

        return normalizedObservation;
    }

    private OffsetDateTime parseDateTime(String dateTimeString) {
        return OffsetDateTime.of(LocalDateTime.parse(dateTimeString), ZoneOffset.UTC);
    }
}
