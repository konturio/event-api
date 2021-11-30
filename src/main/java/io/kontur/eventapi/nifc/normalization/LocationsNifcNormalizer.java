package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.Point;

import java.util.Map;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.GeometryUtil.convertFeatureToFeatureCollection;
import static io.kontur.eventapi.util.GeometryUtil.readFeature;
import static io.kontur.eventapi.util.SeverityUtil.calculateSeverity;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public class LocationsNifcNormalizer extends NifcNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return dataLakeDto.getProvider().equals(NIFC_LOCATIONS_PROVIDER);
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation observation = createObservationFromDataLake(dataLakeDto);

        Feature feature = readFeature(dataLakeDto.getData());
        Map<String, Object> props = feature.getProperties();

        observation.setGeometries(convertFeatureToFeatureCollection(feature));
        observation.setDescription(readString(props, "IncidentShortDescription"));

        long startedAtMilli = readLong(props, "CreatedOnDateTime_dt");
        observation.setStartedAt(getDateTimeFromMilli(startedAtMilli).truncatedTo(SECONDS));

        String name = readString(props, "IncidentName");
        String type = readString(props, "IncidentTypeCategory");
        observation.setName(composeName(name, type));
        observation.setProperName(name);

        Double lon = readDouble(props, "InitialLongitude");
        Double lat = readDouble(props, "InitialLatitude");
        observation.setPoint(makeWktPoint(lon, lat));

        double areaSqKm2 = convertAcresToSqKm(readDouble(props, "CalculatedAcres"));
        long durationHours = between(observation.getStartedAt(), observation.getEndedAt()).toHours();
        observation.setEventSeverity(calculateSeverity(areaSqKm2, durationHours));

        return observation;
    }
}
