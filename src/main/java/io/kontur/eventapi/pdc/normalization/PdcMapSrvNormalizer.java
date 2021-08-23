package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.Map;
import java.util.UUID;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;

@Component
public class PdcMapSrvNormalizer extends PdcHazardNormalizer {

    private final static Logger LOG = LoggerFactory.getLogger(PdcMapSrvNormalizer.class);
    private final static GeoJSONReader reader = new GeoJSONReader();

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return PDC_MAP_SRV_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedObservation = new NormalizedObservation();
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setActive(true);
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setStartedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setEndedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setEventSeverity(Severity.UNKNOWN);
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());

        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        Map<String, Object> properties = feature.getProperties();
        Geometry geometry = feature.getGeometry();

        normalizedObservation.setType(defineType(readString(properties, "type_id")));
        normalizedObservation.setGeometries(convertGeometries(geometry));
        normalizedObservation.setPoint(getCentroid(geometry, normalizedObservation.getObservationId()));
        return normalizedObservation;
    }

    private FeatureCollection convertGeometries(Geometry geometry) {
        return new FeatureCollection(new Feature[] {new Feature(geometry, EXPOSURE_PROPERTIES)});
    }

    private String getCentroid(Geometry geometry, UUID observationID) {
        try {
            Point centroid = reader.read(geometry).getCentroid();
            return makeWktPoint(centroid.getX(), centroid.getY());
        } catch (Exception e) {
            LOG.warn("Can't find center point for observation. Observation ID: {}", observationID);
        }
        return null;
    }
}
