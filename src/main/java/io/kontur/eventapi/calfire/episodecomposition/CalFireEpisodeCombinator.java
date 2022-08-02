package io.kontur.eventapi.calfire.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.WildfireEpisodeCombinator;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;

@Component
public class CalFireEpisodeCombinator extends WildfireEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(CALFIRE_PROVIDER);
    }

    @Override
    public Optional<List<FeedEpisode>> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        return createDefaultEpisode(observation);
    }
}
