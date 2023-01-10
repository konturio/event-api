package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import org.springframework.stereotype.Component;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;

@Component
public class NasaFloodsPdcMapSrvNormalizer extends PdcMapSrvNormalizer {
    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return PDC_MAP_SRV_PROVIDER.equals(dataLakeDto.getProvider()) && isNasaFlood(dataLakeDto);
    }

    @Override
    public boolean isSkipped() {
        return false;
    }
}
