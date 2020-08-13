package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedObservationsDto;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.util.*;

@Component
public class HpSrvMagsNormalizer extends Normalizer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean isApplicable(EventDataLakeDto dataLakeDto) {
        return HpSrvSearchJob.HP_SRV_MAG_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservationsDto normalize(EventDataLakeDto dataLakeDto) {
        NormalizedObservationsDto normalizedDto = new NormalizedObservationsDto();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setExternalId(dataLakeDto.getExternalId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());
        try {
            FeatureCollection fc = mapper.readValue(dataLakeDto.getData(), FeatureCollection.class);
            normalizedDto.setGeometries(convertGeometries(fc).toString());

            if (fc.getFeatures() != null && fc.getFeatures().length > 0) {
                Map<String, Object> props = fc.getFeatures()[0].getProperties();
                normalizedDto.setName(readString(props, "hazard.hazardName"));
                normalizedDto.setDescription(readString(props, "title"));
                normalizedDto.setType(readString(props, "hazard.hazardType.typeId"));
//                normalizedDto.setEventSeverity();  TODO
                normalizedDto.setPoint(makeWktPoint(readDouble(props, "hazard.longitude"),
                        readDouble(props, "hazard.latitude")));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return normalizedDto;
    }

    private FeatureCollection convertGeometries(FeatureCollection input) {
        List<Feature> features = new ArrayList<>(input.getFeatures().length);

        Arrays.stream(input.getFeatures())
                .forEach(feature -> {
                    Map<String, Object> props = feature.getProperties();
                    Map<String, Object> map = new HashMap<>();
                    map.put("description", props.get("title"));
                    //map.put("severity", ) TODO add severity
                    features.add(new Feature(feature.getGeometry(), map));
                });

        return new FeatureCollection(features.toArray(new Feature[0]));
    }
}
