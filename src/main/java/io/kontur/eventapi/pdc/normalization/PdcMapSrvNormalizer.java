package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.util.GeometryUtil;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;

import java.util.Map;

import static io.kontur.eventapi.entity.EventType.FLOOD;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static org.apache.commons.lang3.StringUtils.contains;

@Component
public class PdcMapSrvNormalizer extends PdcHazardNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        return PDC_MAP_SRV_PROVIDER.equals(dataLakeDto.getProvider())
                && !(FLOOD.equals(defineType(readString(feature.getProperties(), "type_id")))
                && contains(readString(feature.getProperties(), "exp_description"), ORIGIN_NASA));
    }

    @Override
    public NormalizedObservation runNormalization(DataLake dataLakeDto) {
        NormalizedObservation observation = new NormalizedObservation();

        observation.setObservationId(dataLakeDto.getObservationId());
        observation.setProvider(dataLakeDto.getProvider());
        observation.setActive(true);
        observation.setLoadedAt(dataLakeDto.getLoadedAt());
        observation.setEventSeverity(Severity.UNKNOWN);
        observation.setExternalEventId(dataLakeDto.getExternalId());

        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        Map<String, Object> properties = feature.getProperties();
        Geometry geometry = feature.getGeometry();

        Long createDate = readLong(properties, "create_date");
        observation.setStartedAt(createDate == null ? dataLakeDto.getLoadedAt() : getDateTimeFromMilli(createDate));
        Long updateDate = readLong(properties, "update_date");
        observation.setEndedAt(updateDate == null ? dataLakeDto.getLoadedAt() : getDateTimeFromMilli(updateDate));
        observation.setSourceUpdatedAt(observation.getEndedAt());

        observation.setType(defineType(readString(properties, "type_id")));
        String description = readString(properties, "exp_description");
        observation.setDescription(description);
        observation.setEpisodeDescription(description);

        observation.setOrigin(observation.getDescription() != null && observation.getDescription().contains(ORIGIN_NASA) ? ORIGIN_NASA : null);

        observation.setGeometries(convertGeometries(geometry));
        observation.setPoint(GeometryUtil.getCentroid(geometry, observation.getObservationId()));

        return observation;
    }

    private FeatureCollection convertGeometries(Geometry geometry) {
        return new FeatureCollection(new Feature[] {new Feature(geometry, EXPOSURE_PROPERTIES)});
    }

}
