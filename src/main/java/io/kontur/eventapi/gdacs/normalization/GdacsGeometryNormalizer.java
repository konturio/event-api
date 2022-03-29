package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.gdacs.converter.GdacsPropertiesConverter;
import org.apache.commons.lang3.StringUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import org.springframework.stereotype.Component;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;

@Component
public class GdacsGeometryNormalizer extends GdacsNormalizer {

    private final GdacsPropertiesConverter propertiesConverter;

    public GdacsGeometryNormalizer(GdacsPropertiesConverter propertiesConverter) {
        this.propertiesConverter = propertiesConverter;
    }

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

        normalizedObservation.setGeometries(computeGeometry(featureCollection));

        String eventType = readString(properties, "eventtype");
        String eventId = readString(properties, "eventid");
        normalizedObservation.setType(defineGeometryType(eventType));
        normalizedObservation.setExternalEventId(composeExternalEventId(eventType, eventId));

        normalizedObservation.setRegion(readString(properties, "country"));
        normalizedObservation.setProperName(readString(properties, "eventname").trim());
        normalizedObservation.setStartedAt(parseDateTime(readString(properties, "fromdate")));
        normalizedObservation.setEndedAt(parseDateTime(readString(properties, "todate")));

        Map<String, Object> urlMap = readMap(properties, "url");
        String url = urlMap == null ? null : readString(urlMap, "report");
        if (StringUtils.isNotBlank(url)) {
            normalizedObservation.setUrls(List.of(url));
        }

        return normalizedObservation;
    }

    private OffsetDateTime parseDateTime(String dateTimeString) {
        return dateTimeString == null ? null : OffsetDateTime.of(LocalDateTime.parse(dateTimeString), ZoneOffset.UTC);
    }

    private FeatureCollection computeGeometry(FeatureCollection fc) {
        Feature[] features = new Feature[fc.getFeatures().length];
        for (int i = 0; i < features.length; i++) {
            Feature feature = fc.getFeatures()[i];

            String eventClass = readString(feature.getProperties(), "Class");
            OffsetDateTime polygonDate = parseDateTime(readString(feature.getProperties(), "polygondate"));
            String polygonLabel = readString(feature.getProperties(), "polygonlabel");
            Boolean forecast = readBoolean(feature.getProperties(), "forecast");

            Map<String, Object> props = propertiesConverter.convertProperties(eventClass, polygonDate, polygonLabel, forecast);
            propertiesConverter.migrateProperties(feature.getProperties(), props);

            features[i] = new Feature(feature.getGeometry(), props);
        }
        return new FeatureCollection(features);
    }
}
