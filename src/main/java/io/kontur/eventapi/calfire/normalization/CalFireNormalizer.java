package io.kontur.eventapi.calfire.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.service.LocationService;
import io.kontur.eventapi.util.DateTimeUtil;
import io.kontur.eventapi.util.GeometryUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONReader;
import org.locationtech.jts.geom.Point;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;
import static io.kontur.eventapi.util.GeometryUtil.*;
import static io.kontur.eventapi.util.SeverityUtil.BURNED_AREA_KM2;
import static io.kontur.eventapi.util.SeverityUtil.CONTAINED_AREA_PCT;

@Component
public class CalFireNormalizer extends Normalizer {

    private final LocationService locationService;
    private final GeoJSONReader geoJSONReader = new GeoJSONReader();

    public CalFireNormalizer(LocationService locationService) {
        this.locationService = locationService;
    }

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
        if (StringUtils.isBlank(normalizedObservation.getRegion())) {
            Point centroid = geoJSONReader.read(geometry).getCentroid();
            normalizedObservation.setRegion(
                    locationService.getLocation(centroid.getX(), centroid.getY()));
        }

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
        double burnedAreaKm2;
        try {
            burnedAreaKm2 = Double.parseDouble(readString(properties, "AcresBurned")) / ACRES_IN_SQ_KM;
            normalizedObservation.getSeverityData().put(BURNED_AREA_KM2, burnedAreaKm2);
        } catch (Exception e) {
            LOG.warn("Can't find burned acres value. Observation ID: {}, value: {}", normalizedObservation.getObservationId(), readString(properties, "AcresBurned"));
            burnedAreaKm2 = 0d;
        }

        try {
            normalizedObservation.getSeverityData().put(CONTAINED_AREA_PCT, Double.parseDouble(readString(properties, "PercentContained")));
        } catch (Exception e) {
            LOG.warn("Can't find contained area percent value. Observation ID: {}, value: {}", normalizedObservation.getObservationId(), readString(properties, "PercentContained"));
        }

        if (duration.compareTo(Duration.ofHours(24L)) < 0) {
            normalizedObservation.setEventSeverity(Severity.MINOR);
        } else {
            if (burnedAreaKm2 < 10) {
                normalizedObservation.setEventSeverity(Severity.MINOR);
            } else if (burnedAreaKm2 < 50) {
                normalizedObservation.setEventSeverity(Severity.MODERATE);
            } else if (burnedAreaKm2 < 100) {
                normalizedObservation.setEventSeverity(Severity.SEVERE);
            } else {
                normalizedObservation.setEventSeverity(Severity.EXTREME);
            }
        }
        return normalizedObservation;
    }
}
