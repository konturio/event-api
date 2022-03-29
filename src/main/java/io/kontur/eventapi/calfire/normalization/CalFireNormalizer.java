package io.kontur.eventapi.calfire.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.util.GeometryUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;
import static io.kontur.eventapi.util.GeometryUtil.*;

@Component
public class CalFireNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(CalFireNormalizer.class);
    private final static String WILDFIRE = "Wildfire";
    private final static double ACRES_IN_SQ_KM = 247.105d;

    public final static Map<String, Object> CALFIRE_PROPERTIES = Map.of(AREA_TYPE_PROPERTY, START_POINT, IS_OBSERVED_PROPERTY, true);

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return CALFIRE_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        Geometry geometry = feature.getGeometry();
        normalizedObservation.setGeometries(convertGeometryToFeatureCollection(geometry, CALFIRE_PROPERTIES));
        normalizedObservation.setPoint(GeometryUtil.getCentroid(geometry, normalizedObservation.getObservationId()));

        Map<String, Object> properties = feature.getProperties();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setType(EventType.WILDFIRE);
        normalizedObservation.setActive(Boolean.valueOf(readString(properties, "IsActive")));
        String url = readString(properties, "Url");
        if (StringUtils.isNotBlank(url)) {
            normalizedObservation.setUrls(List.of(url));
        }
        String name = readString(properties, "Name");
        normalizedObservation.setName(WILDFIRE + " " + name);
        normalizedObservation.setProperName(name);
        normalizedObservation.setRegion(readString(properties, "Location"));

        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        if (StringUtils.isNotBlank(readString(properties, "Started"))) {
            normalizedObservation.setStartedAt(
                    DateTimeUtil.parseDateTimeByPattern(readString(properties, "Started"), null));
        }
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        Duration duration;
        String endedDate = readString(properties, "ExtinguishedDate");
        if (StringUtils.isNotBlank(endedDate)) {
            normalizedObservation.setEndedAt(
                    DateTimeUtil.parseDateTimeByPattern(endedDate, null));
            duration = Duration.between(normalizedObservation.getStartedAt(), normalizedObservation.getEndedAt());
        } else {
            normalizedObservation.setEndedAt(dataLakeDto.getUpdatedAt());
            duration = Duration.between(normalizedObservation.getStartedAt(), OffsetDateTime.now());
        }
        double acresBurned;
        try {
            acresBurned = Double.parseDouble(readString(properties, "AcresBurned"));
        } catch (Exception e) {
            LOG.warn("Can't find burned acres value. Observation ID: {}", normalizedObservation.getObservationId());
            acresBurned = 0d;
        }

        if (duration.compareTo(Duration.ofHours(24L)) < 0) {
            normalizedObservation.setEventSeverity(Severity.MINOR);
        } else {
            if (acresBurned / ACRES_IN_SQ_KM < 10) {
                normalizedObservation.setEventSeverity(Severity.MINOR);
            } else if (acresBurned / ACRES_IN_SQ_KM < 50) {
                normalizedObservation.setEventSeverity(Severity.MODERATE);
            } else if (acresBurned / ACRES_IN_SQ_KM < 100) {
                normalizedObservation.setEventSeverity(Severity.SEVERE);
            } else {
                normalizedObservation.setEventSeverity(Severity.EXTREME);
            }
        }
        return normalizedObservation;
    }
}
