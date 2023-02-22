package io.kontur.eventapi.nifc.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.WildfireEpisodeCombinator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.util.List;
import java.util.Set;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static java.util.Collections.emptyList;

@Component
public class NifcEpisodeCombinator extends WildfireEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return StringUtils.equalsAny(observation.getProvider(), NIFC_LOCATIONS_PROVIDER, NIFC_PERIMETERS_PROVIDER);
    }

    @Override
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        if (episodeExistsForObservation(feedData.getEpisodes(), observation)) {
            return emptyList();
        }
        Set<NormalizedObservation> episodeObservations = findObservationsForEpisode(
                eventObservations, observation.getSourceUpdatedAt(),0L, 0L);
        NormalizedObservation latestObservation = findLatestEpisodeObservation(episodeObservations);
        FeedEpisode episode = createDefaultEpisode(latestObservation);
        episode.setStartedAt(findEpisodeStartedAt(episodeObservations));
        episode.setObservations(mapObservationsToIDs(episodeObservations));
        episode.setGeometries(computeEpisodeGeometries(episodeObservations));
        episode.setDescription(latestObservation.getDescription());
        return List.of(episode);
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
