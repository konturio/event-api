package io.kontur.eventapi.stormsnoaa.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.kontur.eventapi.stormsnoaa.job.StormsNoaaImportJob.STORMS_NOAA_PROVIDER;
import static java.util.Collections.emptyList;

@Component
public class StormsNoaaEpisodeCombinator extends EpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return STORMS_NOAA_PROVIDER.equals(observation.getProvider());
    }

    @Override
    public List<FeedEpisode> processObservation(NormalizedObservation observation,
                                                FeedData feedData,
                                                Set<NormalizedObservation> eventObservations) {
        Set<NormalizedObservation> episodeObservations = eventObservations.stream()
                .filter(obs -> observation.getExternalEventId() != null
                        && observation.getExternalEventId().equals(obs.getExternalEventId()))
                .collect(Collectors.toSet());

        if (episodeObservations.isEmpty() || episodeExists(feedData, episodeObservations)) {
            return emptyList();
        }

        NormalizedObservation latest = findLatestEpisodeObservation(episodeObservations);
        FeedEpisode episode = createDefaultEpisode(latest);
        episode.setStartedAt(findEpisodeStartedAt(episodeObservations));
        episode.setEndedAt(findEpisodeEndedAt(episodeObservations));
        episode.setUpdatedAt(findEpisodeUpdatedAt(episodeObservations));
        episode.setObservations(mapObservationsToIDs(episodeObservations));
        episode.setLocation(findEpisodeLocation(episodeObservations));
        episode.setLoss(findEpisodeLoss(episodeObservations));
        episode.setSeverity(findEpisodeSeverity(episodeObservations));
        episode.setUrls(findEpisodeUrls(episodeObservations));
        episode.setName(findEpisodeName(episodeObservations));
        episode.setDescription(findEpisodeDescription(episodeObservations));
        episode.setGeometries(computeEpisodeGeometries(episodeObservations));
        return List.of(episode);
    }

    private boolean episodeExists(FeedData feedData, Set<NormalizedObservation> observations) {
        Set<UUID> ids = mapObservationsToIDs(observations);
        return feedData.getEpisodes().stream()
                .anyMatch(ep -> ep.getObservations().stream().anyMatch(ids::contains));
    }

    private FeatureCollection computeEpisodeGeometries(Set<NormalizedObservation> observations) {
        Feature[] features = observations.stream()
                .filter(o -> o.getGeometries() != null && o.getGeometries().getFeatures() != null)
                .map(o -> o.getGeometries().getFeatures()[0])
                .distinct()
                .toArray(Feature[]::new);
        return new FeatureCollection(features);
    }
}
