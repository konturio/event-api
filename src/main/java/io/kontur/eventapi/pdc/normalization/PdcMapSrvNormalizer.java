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

import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_NASA_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.getDateTimeFromMilli;
import static org.apache.commons.lang3.StringUtils.contains;

@Component
public class PdcMapSrvNormalizer extends PdcHazardNormalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return List.of(PDC_MAP_SRV_PROVIDER, PDC_MAP_SRV_NASA_PROVIDER).contains(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setEventSeverity(Severity.UNKNOWN);
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());

        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        Map<String, Object> properties = feature.getProperties();
        Geometry geometry = feature.getGeometry();

        Long updateDate = readLong(properties, "update_date");
        normalizedObservation.setSourceUpdatedAt(updateDate == null ? dataLakeDto.getLoadedAt() : getDateTimeFromMilli(updateDate));

        String description = readString(properties, "exp_description");
        normalizedObservation.setDescription(description);
        normalizedObservation.setEpisodeDescription(description);

        normalizedObservation.setOrigin(contains(description, ORIGIN_NASA) ? ORIGIN_NASA : null);

        normalizedObservation.setType(defineType(readString(properties, "type_id")));
        normalizedObservation.setGeometries(convertGeometries(geometry));
        return normalizedObservation;
    }

    private FeatureCollection convertGeometries(Geometry geometry) {
        return new FeatureCollection(new Feature[] {new Feature(geometry, EXPOSURE_PROPERTIES)});
    }

}
