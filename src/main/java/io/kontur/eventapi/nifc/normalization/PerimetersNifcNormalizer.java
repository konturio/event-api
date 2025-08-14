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
import static io.kontur.eventapi.util.SeverityUtil.*;
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
        observation.setDescription(selectFirstNotNull(
                readString(props, "attr_IncidentShortDescription"),
                readString(props, "irwin_IncidentShortDescription")
        ));

        long startedAtMilli = selectFirstNotNull(
                readLong(props, "attr_CreatedOnDateTime_dt"),
                readLong(props, "irwin_CreatedOnDateTime_dt"));
        observation.setStartedAt(getDateTimeFromMilli(startedAtMilli).truncatedTo(SECONDS));

        String name = selectFirstNotNull(readString(props, "attr_IncidentName"), readString(props, "irwin_IncidentName"));
        String type = selectFirstNotNull(readString(props, "attr_IncidentTypeCategory"), readString(props, "irwin_IncidentTypeCategory"));
        observation.setName(composeName(name, type));
        observation.setProperName(name);

        Double lon = selectFirstNotNull(readDouble(props, "attr_InitialLongitude"), readDouble(props, "irwin_InitialLongitude"));
        Double lat = selectFirstNotNull(readDouble(props, "attr_InitialLatitude"), readDouble(props, "irwin_InitialLatitude"));

        double areaSqKm2 = convertAcresToSqKm(readDouble(props, "poly_GISAcres"));
        long durationHours = between(observation.getStartedAt(), observation.getEndedAt()).toHours();
        observation.setEventSeverity(calculateSeverity(areaSqKm2, durationHours));

        observation.getSeverityData().put(BURNED_AREA_KM2, areaSqKm2);

        Double percentContained = readDouble(props, "attr_PercentContained");
        if (percentContained != null) {
            observation.getSeverityData().put(CONTAINED_AREA_PCT, percentContained);
        }

        String uniqueFireId = readString(props, "attr_UniqueFireIdentifier");
        if (uniqueFireId != null) {
            String sourceLink = String.format(
                    "https://services3.arcgis.com/T4QMspbfLg3qTGWY/arcgis/rest/services/WFIGS_Interagency_Perimeters_YearToDate/FeatureServer/0/query?where=attr_UniqueFireIdentifier%%3D%%27%s%%27&outFields=*&f=geojson",
                    uniqueFireId);
            observation.getSeverityData().put("source_link", sourceLink);
        }

        String irwinId = readString(props, "attr_IrwinID");
        if (irwinId != null) {
            observation.getSeverityData().put("IrwinID", irwinId);
        }
        return observation;
    }

    private <T> T selectFirstNotNull(T a, T b) {
        return a != null ? a : b;
    }
}
