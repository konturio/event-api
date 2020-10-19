package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.util.HashMap;
import java.util.Map;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static io.kontur.eventapi.util.JsonUtil.readJson;
import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class HpSrvSearchNormalizer extends PDCHazardNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(HpSrvSearchNormalizer.class);

    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return HP_SRV_SEARCH_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedDto = new NormalizedObservation();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());

        Map<String, Object> props = readJson(dataLakeDto.getData(), new TypeReference<>() {});

        normalizedDto.setExternalEventId(readString(props, "uuid"));
        normalizedDto.setEventSeverity(defineSeverity(readString(props, "severity_ID")));
        normalizedDto.setName(readString(props, "hazard_Name"));
        normalizedDto.setDescription(readString(props, "description"));
        normalizedDto.setEpisodeDescription(readString(props, "description"));
        normalizedDto.setType(defineType(readString(props, "type_ID")));
        normalizedDto.setStartedAt(readDateTime(props, "start_Date"));
        normalizedDto.setEndedAt(readDateTime(props, "end_Date"));
        normalizedDto.setSourceUpdatedAt(readDateTime(props, "update_Date"));
        String pointWkt = makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude"));
        normalizedDto.setPoint(pointWkt);

        try {
            normalizedDto.setGeometries(writeJson(convertGeometry(pointWkt, props)));
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }

        return normalizedDto;
    }

    private FeatureCollection convertGeometry(String point, Map<String, Object> props) throws ParseException {
        Geometry geometry = geoJSONWriter.write(wktReader.read(point));

        Map<String, Object> map = new HashMap<>();
        map.put("description", readString(props, "description"));
        map.put("updatedAt", readDateTime(props, "update_Date"));

        Feature feature = new Feature(geometry, map);

        return new FeatureCollection(new Feature[] {feature});

    }

}
