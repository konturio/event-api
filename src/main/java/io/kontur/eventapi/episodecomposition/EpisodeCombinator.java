package io.kontur.eventapi.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.job.Applicable;

import java.util.Optional;
import java.util.Set;

public abstract class EpisodeCombinator implements Applicable<NormalizedObservation> {

    public abstract Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations);

    protected Optional<FeedEpisode> createDefaultEpisode(NormalizedObservation observation) {
        FeedEpisode feedEpisode = new FeedEpisode();
        feedEpisode.setName(observation.getName());
        feedEpisode.setDescription(observation.getEpisodeDescription());
        feedEpisode.setType(observation.getType());
        feedEpisode.setActive(observation.getActive());
        feedEpisode.setSeverity(observation.getEventSeverity());
        feedEpisode.setStartedAt(observation.getStartedAt());
        feedEpisode.setEndedAt(observation.getEndedAt());
        feedEpisode.setUpdatedAt(observation.getLoadedAt());
        feedEpisode.setSourceUpdatedAt(observation.getSourceUpdatedAt());
        feedEpisode.addObservation(observation.getObservationId());

        if (observation.getGeometries() != null) {
            feedEpisode.setGeometries(observation.getGeometries());
        }

        return Optional.of(feedEpisode);
    }
}
