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
import java.util.UUID;
import java.util.Objects;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_PERIMETERS_PROVIDER;
import static io.kontur.eventapi.util.GeometryUtil.isEqualGeometries;
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
                eventObservations, observation.getSourceUpdatedAt(), 0L, 0L);

        NormalizedObservation latestObservation = findLatestEpisodeObservation(episodeObservations);
        FeedEpisode episode = createDefaultEpisode(latestObservation);
        episode.setStartedAt(findEpisodeStartedAt(episodeObservations));
        episode.setObservations(mapObservationsToIDs(episodeObservations));
        episode.setGeometries(computeEpisodeGeometries(episodeObservations));
        episode.setDescription(latestObservation.getDescription());

        List<FeedEpisode> episodes = feedData.getEpisodes();
        FeedEpisode lastEpisode = episodes.isEmpty() ? null : episodes.get(episodes.size() - 1);
        if (lastEpisode != null && sameEpisodes(lastEpisode, episode)) {
            lastEpisode.setEndedAt(episode.getEndedAt());
            lastEpisode.setSourceUpdatedAt(episode.getSourceUpdatedAt());
            lastEpisode.setUpdatedAt(episode.getUpdatedAt());
            lastEpisode.addObservations(episode.getObservations());
            lastEpisode.addUrlIfNotExists(episode.getUrls());
            return emptyList();
        }

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

    private boolean sameEpisodes(FeedEpisode episode1, FeedEpisode episode2) {
        FeatureCollection geom1 = episode1.getGeometries();
        FeatureCollection geom2 = episode2.getGeometries();
        boolean geometriesEqual = (geom1 == null && geom2 == null)
                || (geom1 != null && geom2 != null && isEqualGeometries(geom1, geom2));

        return StringUtils.equalsIgnoreCase(episode1.getName(), episode2.getName())
                && Objects.equals(episode1.getLoss(), episode2.getLoss())
                && episode1.getSeverity() == episode2.getSeverity()
                && StringUtils.equalsIgnoreCase(episode1.getLocation(), episode2.getLocation())
                && geometriesEqual;
    }

}
