package io.kontur.eventapi.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DefaultEpisodeCombinator extends EpisodeCombinator {

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData) {
        return createDefaultEpisode(observation);
    }
}
