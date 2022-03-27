package io.kontur.eventapi.uhc.normalization;

import static io.kontur.eventapi.uhc.converter.UHCDataLakeConverter.UHC_PROVIDER;
import static io.kontur.eventapi.util.GeometryUtil.AREA_TYPE_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.GLOBAL_AREA;
import static io.kontur.eventapi.util.GeometryUtil.IS_OBSERVED_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.convertGeometryToFeatureCollection;

import java.util.List;
import java.util.Map;

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

@Component
public class HumanitarianCrisisNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(HumanitarianCrisisNormalizer.class);

    public final static Map<String, Object> UHC_PROPERTIES =
            Map.of(AREA_TYPE_PROPERTY, GLOBAL_AREA, IS_OBSERVED_PROPERTY, true);

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return UHC_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());

        Map<String, Object> properties = feature.getProperties();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        Geometry geometry = feature.getGeometry();
        normalizedObservation.setGeometries(convertGeometryToFeatureCollection(geometry, UHC_PROPERTIES));
        normalizedObservation.setPoint(GeometryUtil.getCentroid(geometry, normalizedObservation.getObservationId()));
        try {
            String severity = readString(properties, "severity");
            normalizedObservation.setEventSeverity(Severity.valueOf(severity.toUpperCase()));
        } catch (Exception e) {
            LOG.debug("Error while get severity for Humanitarian crisis {}", dataLakeDto.getExternalId());
        }
        normalizedObservation.setName(readString(properties, "name"));
        normalizedObservation.setDescription(readString(properties, "description"));
        normalizedObservation.setEpisodeDescription(readString(properties, "description"));
        try {
            String type = readString(properties, "type");
            normalizedObservation.setType(EventType.valueOf(type.toUpperCase()));
        } catch (Exception e) {
            LOG.debug("Error while get type for Humanitarian crisis {}", dataLakeDto.getExternalId());
        }
        normalizedObservation.setActive(null);
        normalizedObservation.setRegion(readString(properties, "location"));
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        try {
            if (StringUtils.isNotBlank(readString(properties, "started_at"))) {
                normalizedObservation.setStartedAt(
                        DateTimeUtil.parseDateTimeByPattern(readString(properties, "started_at"),
                                DateTimeUtil.UHC_DATETIME_PATTERN));
            }
        } catch (Exception e) {
            LOG.debug("Error while get started_at for Humanitarian crisis {}", dataLakeDto.getExternalId());
        }
        try {
            if (StringUtils.isNotBlank(readString(properties, "ended_at"))) {
                normalizedObservation.setEndedAt(
                        DateTimeUtil.parseDateTimeByPattern(readString(properties, "ended_at"),
                                DateTimeUtil.UHC_DATETIME_PATTERN));
            } else {
                normalizedObservation.setEndedAt(normalizedObservation.getStartedAt());
            }
        } catch (Exception e) {
            LOG.debug("Error while get ended_at for Humanitarian crisis {}", dataLakeDto.getExternalId());
        }
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        Object urls = properties.get("urls");
        if (urls instanceof List) {
            normalizedObservation.setSourceUri((List<String>) urls);
        } else if (urls instanceof String) {
            normalizedObservation.setSourceUri(List.of(String.valueOf(urls)));
        }
        normalizedObservation.setExternalEpisodeId(null);
        normalizedObservation.setProperName(readString(properties, "name"));

        return normalizedObservation;
    }

}
