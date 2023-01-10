package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.kontur.eventapi.entity.EventType.FLOOD;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static org.apache.commons.lang3.StringUtils.contains;

@Component
public class NasaFloodsPdcSqsMessageNormalizer extends PdcSqsMessageNormalizer {
    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        if (PDC_SQS_PROVIDER.equals(dataLakeDto.getProvider())) {
            Map<String, Object> props = parseProps(parseEvent(dataLakeDto.getData()));
            return FLOOD.equals(defineType(readString((Map<String, Object>) props.get("hazardType"), "typeId")))
                    && contains(readString((Map<String, Object>) props.get("hazardDescription"), "description"), ORIGIN_NASA);
        }
        return false;
    }

    @Override
    protected boolean isObservationSkipped() {
        return false;
    }
}
