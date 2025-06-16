package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;

import java.util.Map;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.GeometryUtil.convertGeometryToFeatureCollection;
import static io.kontur.eventapi.util.GeometryUtil.readFeature;
import static io.kontur.eventapi.util.SeverityUtil.*;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public class LocationsNifcNormalizer extends NifcNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(LocationsNifcNormalizer.class);

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return dataLakeDto.getProvider().equals(NIFC_LOCATIONS_PROVIDER);
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation observation = createObservationFromDataLake(dataLakeDto);

        Feature feature = readFeature(dataLakeDto.getData());
        Map<String, Object> props = feature.getProperties();

        observation.setGeometries(convertGeometryToFeatureCollection(feature.getGeometry(), LOCATIONS_PROPERTIES));
        observation.setDescription(readString(props, "IncidentShortDescription"));
        observation.setEpisodeDescription(readString(props, "IncidentShortDescription"));

        Long startedAtMilli = tryReadLong(props, "FireDiscoveryDateTime");
        if (startedAtMilli == null) {
            LOG.warn("Couldn't parse FireDiscoveryDateTime for {}", observation.getObservationId());
            startedAtMilli = tryReadLong(props, "CreatedOnDateTime_dt");
        }
        if (startedAtMilli != null) {
            observation.setStartedAt(getDateTimeFromMilli(startedAtMilli).truncatedTo(SECONDS));
        }

        Long fireOutMilli = tryReadLong(props, "FireOutDateTime");
        if (fireOutMilli != null) {
            observation.setEndedAt(getDateTimeFromMilli(fireOutMilli).truncatedTo(SECONDS));
            observation.setActive(false);
        } else {
            observation.setActive(true);
        }

        observation.setCost(tryReadBigDecimal(props, "EstimatedCostToDate"));

        String name = readString(props, "IncidentName");
        String type = readString(props, "IncidentTypeCategory");
        observation.setName(composeName(name, type));
        observation.setProperName(name);

        Double lon = readDouble(props, "InitialLongitude");
        Double lat = readDouble(props, "InitialLatitude");
        observation.setPoint(makeWktPoint(lon, lat));

        Double calculatedAcres = readDouble(props, "CalculatedAcres");
        Double incidentSize = readDouble(props, "IncidentSize");
        Double finalAcres = calculatedAcres == null ? incidentSize : calculatedAcres;
        double areaSqKm2 = convertAcresToSqKm(finalAcres);
        long durationHours = between(observation.getStartedAt(), observation.getEndedAt()).toHours();
        observation.setEventSeverity(calculateSeverity(areaSqKm2, durationHours));

        if (finalAcres != null) {
            observation.getSeverityData().put(BURNED_AREA_KM2, areaSqKm2);
        }

        Double percentContained = readDouble(props, "PercentContained");
        if (percentContained != null) {
            observation.getSeverityData().put(CONTAINED_AREA_PCT, percentContained);
        }

        return observation;
    }
}
