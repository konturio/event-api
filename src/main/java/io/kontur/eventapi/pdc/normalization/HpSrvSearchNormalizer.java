package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dto.EventDataLakeDto;
import io.kontur.eventapi.dto.NormalizedObservationsDto;
import io.kontur.eventapi.normalization.Normalizer;
import io.kontur.eventapi.pdc.job.HpSrvSearchJob;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HpSrvSearchNormalizer extends Normalizer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean isApplicable(EventDataLakeDto dataLakeDto) {
        return HpSrvSearchJob.HP_SRV_SEARCH_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservationsDto normalize(EventDataLakeDto dataLakeDto) {
        NormalizedObservationsDto normalizedDto = new NormalizedObservationsDto();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setExternalId(dataLakeDto.getExternalId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());
//                normalizedDto.setEventSeverity();  TODO

        try {
            Map<String, Object> props = mapper.readValue(dataLakeDto.getData(), new TypeReference<>() {});

            normalizedDto.setName(readString(props, "hazard_Name"));
            normalizedDto.setDescription(readString(props, "description"));
            //                normalizedDto.setEventSeverity();  TODO
            normalizedDto.setType(readString(props, "type_ID"));
            normalizedDto.setPoint(makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return normalizedDto;
    }

}
