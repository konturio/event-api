package io.kontur.eventapi.pdc.episodecomposition;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;

import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PdcEpisodeCombinator extends BasePdcEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return StringUtils.equalsAny(observation.getProvider(),
                HP_SRV_SEARCH_PROVIDER, PDC_SQS_PROVIDER, PDC_MAP_SRV_PROVIDER);
    }

}
