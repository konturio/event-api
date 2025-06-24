package io.kontur.eventapi.usgs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSONFactory;

import java.util.HashMap;
import java.util.Map;

import static io.kontur.eventapi.usgs.converter.UsgsShakeMapDataLakeConverter.USGS_SHAKEMAP_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.*;

@Component
public class UsgsShakeMapNormalizer extends Normalizer {

    private static final Map<String, Object> GEOMETRY_PROPERTIES = Map.of(
            AREA_TYPE_PROPERTY, ALERT_AREA,
            IS_OBSERVED_PROPERTY, true
    );

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return USGS_SHAKEMAP_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        Map<String, Object> props = feature.getProperties();

        NormalizedObservation o = new NormalizedObservation();
        o.setObservationId(dataLakeDto.getObservationId());
        o.setProvider(dataLakeDto.getProvider());
        o.setLoadedAt(dataLakeDto.getLoadedAt());
        o.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        o.setExternalEventId(feature.getId());
        o.setType(EventType.EARTHQUAKE);
        o.setName(readString(props, "title"));
        o.setDescription(readString(props, "place"));
        o.setStartedAt(getDateTimeFromMilli(readLong(props, "time")));
        o.setEndedAt(getDateTimeFromMilli(readLong(props, "updated")));
        o.setGeometries(convertGeometryToFeatureCollection(feature.getGeometry(), GEOMETRY_PROPERTIES));

        Double mag = readDouble(props, "mag");
        Double depth = null;
        if (feature.getGeometry() instanceof org.wololo.geojson.Point p && p.getCoordinates().length > 2) {
            depth = p.getCoordinates()[2];
        }
        Map<String, Object> severityData = new HashMap<>();
        if (mag != null) {
            severityData.put(MAGNITUDE, mag);
        }
        if (depth != null) {
            severityData.put(DEPTH_KM, depth);
        }
        o.setSeverityData(severityData);
        o.setEventSeverity(calcSeverity(mag));
        return o;
    }

    private Severity calcSeverity(Double mag) {
        if (mag == null) {
            return Severity.UNKNOWN;
        } else if (mag < 5.0) {
            return Severity.MINOR;
        } else if (mag < 6.5) {
            return Severity.MODERATE;
        } else if (mag < 7.5) {
            return Severity.SEVERE;
        } else {
            return Severity.EXTREME;
        }
    }
}
