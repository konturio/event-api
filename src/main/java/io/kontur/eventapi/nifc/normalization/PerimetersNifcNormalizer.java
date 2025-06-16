package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import java.util.Map;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.GeometryUtil.convertGeometryToFeatureCollection;
import static io.kontur.eventapi.util.GeometryUtil.readFeature;
import static io.kontur.eventapi.util.SeverityUtil.*;
import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public class PerimetersNifcNormalizer extends NifcNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(PerimetersNifcNormalizer.class);

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
        String shortDesc = selectFirstNotNull(
                readString(props, "irwin_IncidentShortDescription"),
                readString(props, "attr_IncidentShortDescription")
        );
        observation.setDescription(shortDesc);
        observation.setEpisodeDescription(shortDesc);

        Long startedAtMilli = selectFirstNotNull(
                tryReadLong(props, "irwin_FireDiscoveryDateTime"),
                tryReadLong(props, "attr_FireDiscoveryDateTime")
        );
        if (startedAtMilli == null) {
            LOG.warn("Couldn't parse FireDiscoveryDateTime for {}", observation.getObservationId());
            startedAtMilli = selectFirstNotNull(
                    tryReadLong(props, "irwin_CreatedOnDateTime_dt"),
                    tryReadLong(props, "attr_CreatedOnDateTime_dt")
            );
        }
        if (startedAtMilli != null) {
            observation.setStartedAt(getDateTimeFromMilli(startedAtMilli).truncatedTo(SECONDS));
        }

        Long fireOutMilli = selectFirstNotNull(
                tryReadLong(props, "irwin_FireOutDateTime"),
                tryReadLong(props, "attr_FireOutDateTime")
        );
        if (fireOutMilli != null) {
            observation.setEndedAt(getDateTimeFromMilli(fireOutMilli).truncatedTo(SECONDS));
            observation.setActive(false);
        } else {
            observation.setActive(true);
        }

        observation.setCost(selectFirstNotNull(
                tryReadBigDecimal(props, "irwin_EstimatedCostToDate"),
                tryReadBigDecimal(props, "attr_EstimatedCostToDate")
        ));

        String name = selectFirstNotNull(readString(props, "irwin_IncidentName"), readString(props, "attr_IncidentName"));
        String type = selectFirstNotNull(readString(props, "irwin_IncidentTypeCategory"), readString(props, "attr_IncidentTypeCategory"));
        observation.setName(composeName(name, type));
        observation.setProperName(name);

        Double lon = selectFirstNotNull(readDouble(props, "irwin_InitialLongitude"), readDouble(props, "attr_InitialLongitude"));
        Double lat = selectFirstNotNull(readDouble(props, "irwin_InitialLatitude"), readDouble(props, "attr_InitialLatitude"));
        observation.setPoint(makeWktPoint(lon, lat));

        double areaSqKm2 = convertAcresToSqKm(readDouble(props, "poly_GISAcres"));
        long durationHours = between(observation.getStartedAt(), observation.getEndedAt()).toHours();
        observation.setEventSeverity(calculateSeverity(areaSqKm2, durationHours));

        observation.getSeverityData().put(BURNED_AREA_KM2, areaSqKm2);

        Double percentContained = selectFirstNotNull(
                readDouble(props, "irwin_PercentContained"),
                readDouble(props, "attr_PercentContained")
        );
        if (percentContained != null) {
            observation.getSeverityData().put(CONTAINED_AREA_PCT, percentContained);
        }
        return observation;
    }

    private <T> T selectFirstNotNull(T a, T b) {
        return a != null ? a : b;
    }
}
