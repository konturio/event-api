package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static io.kontur.eventapi.util.JsonUtil.readJson;

@Component
public class HpSrvSearchNormalizer extends PdcHazardNormalizer {

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
        normalizedDto.setExternalEpisodeId(readString(props, "uuid"));
        normalizedDto.setEventSeverity(defineSeverity(readString(props, "severity_ID")));
        normalizedDto.setName(readString(props, "hazard_Name"));
        normalizedDto.setDescription(readString(props, "description"));
        normalizedDto.setEpisodeDescription(readString(props, "description"));
        normalizedDto.setType(defineType(readString(props, "type_ID")));
        normalizedDto.setSourceUpdatedAt(readDateTime(props, "update_Date"));
        String url = readString(props, "snc_url");
        if (StringUtils.isNotBlank(url)) {
            normalizedDto.setSourceUri(List.of(url));
        }

        OffsetDateTime startedAt = readDateTime(props, "start_Date");
        OffsetDateTime endedAt = readDateTime(props, "end_Date");
        normalizedDto.setStartedAt(startedAt != null ? startedAt : endedAt);
        normalizedDto.setEndedAt(endedAt != null ? endedAt : startedAt);

        String pointWkt = makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude"));
        normalizedDto.setPoint(pointWkt);

        try {
            normalizedDto.setGeometries(convertGeometry(pointWkt, props));
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }

        return normalizedDto;
    }

    private FeatureCollection convertGeometry(String point, Map<String, Object> props) throws ParseException {
        Geometry geometry = geoJSONWriter.write(wktReader.read(point));
        Feature feature = new Feature(geometry, HAZARD_PROPERTIES);

        return new FeatureCollection(new Feature[] {feature});

    }

}
