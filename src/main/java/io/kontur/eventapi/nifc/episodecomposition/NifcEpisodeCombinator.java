package io.kontur.eventapi.nifc.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

@Component
public class NifcEpisodeCombinator extends EpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return StringUtils.equalsAny(observation.getProvider(), NIFC_LOCATIONS_PROVIDER, NIFC_PERIMETERS_PROVIDER);
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        if (episodeExistsForObservation(feedData.getEpisodes(), observation)) {
            return Optional.empty();
        }
        Set<NormalizedObservation> episodeObservations = findObservationsForEpisode(eventObservations, observation.getSourceUpdatedAt());
        NormalizedObservation latestObservation = findLatestEpisodeObservation(episodeObservations);
        FeedEpisode episode = createDefaultEpisode(latestObservation).get();

        episode.setStartedAt(findEpisodeStartedAt(episodeObservations));
        episode.setObservations(mapObservationsToIDs(episodeObservations));
        episode.setGeometries(computeEpisodeGeometries(episodeObservations));
        episode.setDescription(latestObservation.getDescription());

        return Optional.of(episode);
    }

    private boolean episodeExistsForObservation(List<FeedEpisode> eventEpisodes, NormalizedObservation observation) {
        return eventEpisodes
                .stream()
                .anyMatch(episode -> episode.getSourceUpdatedAt().equals(observation.getSourceUpdatedAt()));
    }

    private Set<NormalizedObservation> findObservationsForEpisode(Set<NormalizedObservation> eventObservations, OffsetDateTime sourceUpdateAt) {
        return eventObservations
                .stream()
                .filter(observation -> observation.getSourceUpdatedAt().equals(sourceUpdateAt))
                .collect(toSet());
    }

    private NormalizedObservation findLatestEpisodeObservation(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .max(comparing(NormalizedObservation::getEndedAt))
                .orElse(null);
    }

    private OffsetDateTime findEpisodeStartedAt(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getStartedAt)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
    }

    private Set<UUID> mapObservationsToIDs(Set<NormalizedObservation> observations) {
        return observations
                .stream()
                .map(NormalizedObservation::getObservationId)
                .collect(toSet());
    }

    private FeatureCollection computeEpisodeGeometries(Set<NormalizedObservation> episodeObservations) {
        Feature[] features = episodeObservations
                .stream()
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .distinct()
                .toArray(Feature[]::new);
        return new FeatureCollection(features);
    }

    @Override
    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        if (episodes.size() > 1) {
            episodes.sort(comparing(FeedEpisode::getEndedAt));
            for (int i = 1; i < episodes.size(); i++) {
                episodes.get(i).setStartedAt(episodes.get(i - 1).getEndedAt());
            }
        }
        return episodes;
    }
}
