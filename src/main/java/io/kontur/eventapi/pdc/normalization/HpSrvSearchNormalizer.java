package io.kontur.eventapi.pdc.normalization;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static io.kontur.eventapi.util.JsonUtil.readJson;

@Component
public class HpSrvSearchNormalizer extends Normalizer {

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return HP_SRV_SEARCH_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        NormalizedObservation normalizedDto = new NormalizedObservation();
        normalizedDto.setObservationId(dataLakeDto.getObservationId());
        normalizedDto.setExternalEventId(dataLakeDto.getExternalId());
        normalizedDto.setProvider(dataLakeDto.getProvider());
        normalizedDto.setLoadedAt(dataLakeDto.getLoadedAt());

        Map<String, Object> props = readJson(dataLakeDto.getData(), new TypeReference<>() {});

        normalizedDto.setEventSeverity(readString(props, "severity_ID"));
        normalizedDto.setName(readString(props, "hazard_Name"));
        normalizedDto.setDescription(readString(props, "description"));
        normalizedDto.setType(readString(props, "type_ID"));
        normalizedDto.setStartedAt(readDateTime(props, "start_Date"));
        normalizedDto.setEndedAt(readDateTime(props, "end_Date"));
        normalizedDto.setUpdatedAt(readDateTime(props, "update_Date"));
        normalizedDto.setPoint(makeWktPoint(readDouble(props, "longitude"), readDouble(props, "latitude")));

        return normalizedDto;
    }
}
