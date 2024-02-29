package io.kontur.eventapi.pdc.episodecomposition;

import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.*;

@Component
public class PdcEpisodeCombinator extends BasePdcEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return StringUtils.equalsAny(observation.getProvider(),
                HP_SRV_SEARCH_PROVIDER, PDC_SQS_PROVIDER, PDC_MAP_SRV_PROVIDER, PDC_SQS_NASA_PROVIDER, PDC_MAP_SRV_NASA_PROVIDER);
    }

}
