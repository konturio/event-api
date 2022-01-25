package io.kontur.eventapi.nifc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;

import java.util.Map;

import static io.kontur.eventapi.util.GeometryUtil.*;

public abstract class NifcNormalizer extends Normalizer {

    private static final double acresInSqKm = 247.105;

    protected static final Map<String, Object> LOCATIONS_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, START_POINT, IS_OBSERVED_PROPERTY, true);
    protected static final Map<String, Object> PERIMETERS_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, EXPOSURE, IS_OBSERVED_PROPERTY, true);

    protected NormalizedObservation createObservationFromDataLake(DataLake dataLake) {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setObservationId(dataLake.getObservationId());
        observation.setProvider(dataLake.getProvider());
        observation.setLoadedAt(dataLake.getLoadedAt());
        observation.setSourceUpdatedAt(dataLake.getUpdatedAt());
        observation.setEndedAt(dataLake.getUpdatedAt());
        observation.setExternalEventId(dataLake.getExternalId());
        observation.setType(EventType.WILDFIRE);
        return observation;
    }

    protected double convertAcresToSqKm(Double acres) {
        if (acres == null || acres == 0) {
            return 0;
        }
        return acres / acresInSqKm;
    }

    protected String composeName(String name, String type) {
        String wildfireName = (name == null) ? "" : " " + name;
        if (type != null && type.equals("PX")) {
            return "Prescribed Fire" + wildfireName;
        }
        return "Wildfire" + wildfireName;
    }
}
