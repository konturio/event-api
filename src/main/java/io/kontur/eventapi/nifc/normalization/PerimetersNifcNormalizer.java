package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import java.util.Map;
import java.util.Optional;

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
    public Optional<NormalizedObservation> normalize(DataLake dataLakeDto) {
        NormalizedObservation observation = createObservationFromDataLake(dataLakeDto);

        Feature feature = readFeature(dataLakeDto.getData());
        Map<String, Object> props = feature.getProperties();

        observation.setGeometries(convertGeometryToFeatureCollection(feature.getGeometry(), PERIMETERS_PROPERTIES));
        observation.setDescription(readString(props, "irwin_IncidentShortDescription"));

        long startedAtMilli = readLong(props, "irwin_CreatedOnDateTime_dt");
        observation.setStartedAt(getDateTimeFromMilli(startedAtMilli).truncatedTo(SECONDS));

        String name = readString(props, "poly_IncidentName");
        String type = readString(props, "irwin_IncidentTypeCategory");
        observation.setName(composeName(name, type));
        observation.setProperName(name);

        Double lon = readDouble(props, "irwin_InitialLongitude");
        Double lat = readDouble(props, "irwin_InitialLatitude");
        observation.setPoint(makeWktPoint(lon, lat));

        double areaSqKm2 = convertAcresToSqKm(readDouble(props, "poly_GISAcres"));
        long durationHours = between(observation.getStartedAt(), observation.getEndedAt()).toHours();
        observation.setEventSeverity(calculateSeverity(areaSqKm2, durationHours));

        return Optional.of(observation);
    }
}
