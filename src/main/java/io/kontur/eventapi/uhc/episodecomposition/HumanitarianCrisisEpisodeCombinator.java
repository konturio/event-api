package io.kontur.eventapi.uhc.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.jts2geojson.GeoJSONReader;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.uhc.converter.UHCDataLakeConverter.UHC_PROVIDER;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;

@Component
public class HumanitarianCrisisEpisodeCombinator extends EpisodeCombinator {

    private final GeoJSONReader geoJSONReader = new GeoJSONReader();

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(UHC_PROVIDER);
    }

    @Override
    public Optional<List<FeedEpisode>> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        if (isObservationLinkedToEpisode(observation.getObservationId(), feedData.getEpisodes())) return empty();
        if (eventObservations.size() == 1) return createDefaultEpisode(observation);

        List<NormalizedObservation> observationsNotLinkedToEpisode = findObservationsNotLinkedToEpisode(eventObservations, feedData.getEpisodes());

        List<NormalizedObservation> episodeObservations = findEpisodeObservations(observationsNotLinkedToEpisode);

        Optional<List<FeedEpisode>> episodeOpt = createDefaultEpisode(episodeObservations.get(0));
        episodeOpt.ifPresent(episode -> {
            episode.get(0).setStartedAt(findEpisodeStartedAt(feedData.getEpisodes(), episodeObservations));
            NormalizedObservation latestEpisodeObservation = findLatestEpisodeObservation(new HashSet<>(episodeObservations));
            episode.get(0).setSourceUpdatedAt(latestEpisodeObservation.getSourceUpdatedAt());
            episode.get(0).setEndedAt(latestEpisodeObservation.getSourceUpdatedAt());
            episode.get(0).setObservations(episodeObservations.stream().map(NormalizedObservation::getObservationId).collect(toSet()));
        });

        return episodeOpt;
    }

    private boolean isObservationLinkedToEpisode(UUID observationId, List<FeedEpisode> episodes) {
        return episodes.stream().anyMatch(episode -> episode.getObservations().contains(observationId));
    }

    private List<NormalizedObservation> findObservationsNotLinkedToEpisode(Set<NormalizedObservation> eventObservations, List<FeedEpisode> episodes) {
        return eventObservations
                .stream()
                .filter(observation -> !isObservationLinkedToEpisode(observation.getObservationId(), episodes))
                .sorted(comparing(NormalizedObservation::getSourceUpdatedAt))
                .toList();
    }

    private List<NormalizedObservation> findEpisodeObservations(List<NormalizedObservation> observations) {
        List<NormalizedObservation> episodeObservations = new ArrayList<>();
        episodeObservations.add(observations.get(0));
        for (int i = 1; i < observations.size(); i++) {
            if (sameEpisodeObservations(observations.get(i - 1), observations.get(i)))
                episodeObservations.add(observations.get(i));
            else
                break;
        }
        return episodeObservations;
    }

    private boolean sameEpisodeObservations(NormalizedObservation observation1, NormalizedObservation observation2) {
        if (observation1.getGeometries().getFeatures().length != observation2.getGeometries().getFeatures().length) return false;
        for (int i = 0; i < observation1.getGeometries().getFeatures().length; i++) {
            Feature feature1 = observation1.getGeometries().getFeatures()[i];
            Feature feature2 = observation2.getGeometries().getFeatures()[i];
            if (!feature1.getProperties().equals(feature2.getProperties())) return false;
            if (!geoJSONReader.read(feature1.getGeometry()).equals(geoJSONReader.read(feature2.getGeometry()))) return false;

        }
        return
                observation1.getEventSeverity().equals(observation2.getEventSeverity()) &&
                observation1.getName().equals(observation2.getName()) &&
                observation1.getProperName().equals(observation2.getProperName()) &&
                observation1.getDescription().equals(observation2.getDescription()) &&
                observation1.getEpisodeDescription().equals(observation2.getEpisodeDescription()) &&
                observation1.getType().equals(observation2.getType()) &&
                observation1.getRegion().equals(observation2.getRegion()) &&
                observation1.getUrls().equals(observation2.getUrls());
    }

    private OffsetDateTime findEpisodeStartedAt(List<FeedEpisode> episodes, List<NormalizedObservation> episodeObservations) {
        if (episodes.size() == 0) {
            return episodeObservations
                    .stream()
                    .map(NormalizedObservation::getStartedAt)
                    .min(OffsetDateTime::compareTo)
                    .orElseThrow();
        } else {
            return episodes
                    .stream()
                    .map(FeedEpisode::getSourceUpdatedAt)
                    .max(OffsetDateTime::compareTo)
                    .orElseThrow();
        }
    }
}
