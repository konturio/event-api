package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import org.springframework.stereotype.Component;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;

@Component
public class NasaFloodsPdcSqsMessageNormalizer extends PdcSqsMessageNormalizer {
    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return PDC_SQS_PROVIDER.equals(dataLakeDto.getProvider()) && isNasaFlood(dataLakeDto);
    }

    @Override
    public boolean isSkipped() {
        return false;
    }
}
