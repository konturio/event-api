package io.kontur.eventapi.emdat.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static io.kontur.eventapi.emdat.jobs.EmDatImportJob.EM_DAT_PROVIDER;

/**
 * This class creates episode only for the latest observation of the event.
 * So EM-DAT event always contains one episode with one latest observation.
 */
@Component
public class EmDatEpisodeCombinator extends EpisodeCombinator {

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        OffsetDateTime latestLoadedAt = eventObservations
                .stream()
                .map(NormalizedObservation::getLoadedAt)
                .max(OffsetDateTime::compareTo)
                .get();
        return observation.getLoadedAt().equals(latestLoadedAt) ? createDefaultEpisode(observation) : Optional.empty();
    }

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(EM_DAT_PROVIDER);
    }
}
