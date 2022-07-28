package io.kontur.eventapi.nifc.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.WildfireEpisodeCombinator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;

@Component
public class NifcEpisodeCombinator extends WildfireEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return StringUtils.equalsAny(observation.getProvider(), NIFC_LOCATIONS_PROVIDER, NIFC_PERIMETERS_PROVIDER);
    }

    @Override
    public Optional<List<FeedEpisode>> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        if (episodeExistsForObservation(feedData.getEpisodes(), observation)) {
            return Optional.empty();
        }
        Set<NormalizedObservation> episodeObservations = findObservationsForEpisode(
                eventObservations, observation.getSourceUpdatedAt(),0L, 0L);
        NormalizedObservation latestObservation = findLatestEpisodeObservation(episodeObservations);
        Optional<List<FeedEpisode>> episode = createDefaultEpisode(latestObservation);
        episode.ifPresent(ep -> {
            if (!CollectionUtils.isEmpty(ep)) {
                ep.get(0).setStartedAt(findEpisodeStartedAt(episodeObservations));
                ep.get(0).setObservations(mapObservationsToIDs(episodeObservations));
                ep.get(0).setGeometries(computeEpisodeGeometries(episodeObservations));
                ep.get(0).setDescription(latestObservation.getDescription());
            }
        });
        return episode;
    }

    private FeatureCollection computeEpisodeGeometries(Set<NormalizedObservation> episodeObservations) {
        Feature[] features = episodeObservations
                .stream()
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .distinct()
                .toArray(Feature[]::new);
        return new FeatureCollection(features);
    }

}
