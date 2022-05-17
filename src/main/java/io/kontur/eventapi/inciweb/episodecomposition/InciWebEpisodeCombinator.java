package io.kontur.eventapi.inciweb.episodecomposition;

import static io.kontur.eventapi.inciweb.converter.InciWebDataLakeConverter.INCIWEB_PROVIDER;

import java.util.Optional;
import java.util.Set;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.WildfireEpisodeCombinator;
import org.springframework.stereotype.Component;

@Component
public class InciWebEpisodeCombinator extends WildfireEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(INCIWEB_PROVIDER);
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData,
                                                    Set<NormalizedObservation> eventObservations) {
        return createDefaultEpisode(observation, feedData);
    }
}
