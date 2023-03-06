package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import java.util.Map;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.GeometryUtil.convertGeometryToFeatureCollection;
import static io.kontur.eventapi.util.GeometryUtil.readFeature;
import static io.kontur.eventapi.util.SeverityUtil.calculateSeverity;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public class PerimetersNifcNormalizer extends NifcNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return dataLakeDto.getProvider().equals(NIFC_PERIMETERS_PROVIDER);
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation observation = createObservationFromDataLake(dataLakeDto);

        Feature feature = readFeature(dataLakeDto.getData());
        Map<String, Object> props = feature.getProperties();

        observation.setGeometries(convertGeometryToFeatureCollection(feature.getGeometry(), PERIMETERS_PROPERTIES));
        observation.setDescription(readString(props, "attr_IncidentShortDescription"));

        long startedAtMilli = readLong(props, "attr_CreatedOnDateTime_dt");
        observation.setStartedAt(getDateTimeFromMilli(startedAtMilli).truncatedTo(SECONDS));

        String name = readString(props, "attr_IncidentName");
        String type = readString(props, "attr_IncidentTypeCategory");
        observation.setName(composeName(name, type));
        observation.setProperName(name);

        Double lon = readDouble(props, "attr_InitialLongitude");
        Double lat = readDouble(props, "attr_InitialLatitude");
        observation.setPoint(makeWktPoint(lon, lat));

        double areaSqKm2 = convertAcresToSqKm(readDouble(props, "poly_GISAcres"));
        long durationHours = between(observation.getStartedAt(), observation.getEndedAt()).toHours();
        observation.setEventSeverity(calculateSeverity(areaSqKm2, durationHours));

        return observation;
    }
}
