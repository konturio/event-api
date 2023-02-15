package io.kontur.eventapi.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DefaultEpisodeCombinator extends EpisodeCombinator {

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        return List.of(createDefaultEpisode(observation));
    }
}
