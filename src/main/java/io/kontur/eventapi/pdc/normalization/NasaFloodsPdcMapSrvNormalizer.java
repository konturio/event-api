package io.kontur.eventapi.pdc.normalization;

import io.kontur.eventapi.entity.DataLake;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSONFactory;

import static io.kontur.eventapi.entity.EventType.FLOOD;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;

@Component
public class NasaFloodsPdcMapSrvNormalizer extends PdcMapSrvNormalizer {
    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        Feature feature = (Feature) GeoJSONFactory.create(dataLakeDto.getData());
        return PDC_MAP_SRV_PROVIDER.equals(dataLakeDto.getProvider())
                && FLOOD.equals(defineType(readString(feature.getProperties(), "type_id")))
                && StringUtils.contains(ORIGIN_NASA, readString(feature.getProperties(), "exp_description"));
    }

    @Override
    protected boolean isObservationSkipped() {
        return false;
    }
}
