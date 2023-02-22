package io.kontur.eventapi.emdat.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.kontur.eventapi.emdat.jobs.EmDatImportJob.EM_DAT_PROVIDER;
import static java.util.Collections.emptyList;

/**
 * This class creates episode only for the latest observation of the event.
 * So EM-DAT event always contains one episode with one latest observation.
 */
@Component
public class EmDatEpisodeCombinator extends EpisodeCombinator {

    @Override
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        Optional<OffsetDateTime> latestLoadedAt = eventObservations
                .stream()
                .map(NormalizedObservation::getLoadedAt)
                .max(OffsetDateTime::compareTo);
        return (latestLoadedAt.isPresent() && observation.getLoadedAt().equals(latestLoadedAt.get()))
                ? List.of(createDefaultEpisode(observation)) : emptyList();
    }

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(EM_DAT_PROVIDER);
    }
}
