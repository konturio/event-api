package io.kontur.eventapi.pdc.episodecomposition;

import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_SEARCH_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_MAP_SRV_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static java.util.Comparator.comparing;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.bouncycastle.util.Arrays;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

public abstract class BasePdcEpisodeCombinator extends EpisodeCombinator {

    protected static final long TIME_RANGE_IN_SEC = 90;
    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData,
                                                    Set<NormalizedObservation> eventObservations) {
        if (episodeExistsForObservation(feedData.getEpisodes(), observation)) {
            return Optional.empty();
        }
        Set<NormalizedObservation> episodeObservations = findObservationsForEpisode(eventObservations,
                observation.getSourceUpdatedAt());
        NormalizedObservation latestObservation = findLatestEpisodeObservation(episodeObservations);
        Optional<FeedEpisode> episode = createDefaultEpisode(latestObservation);
        if (episode.isPresent()) {
            episode.get().setStartedAt(findEpisodeStartedAt(episodeObservations));
            episode.get().setEndedAt(findEpisodeEndedAt(episodeObservations));
            episode.get().setUpdatedAt(findEpisodeUpdatedAt(episodeObservations));
            episode.get().setObservations(mapObservationsToIDs(episodeObservations));
            episode.get().setGeometries(computeEpisodeGeometries(episodeObservations));
            episode.get().setDescription(latestObservation.getDescription());
        }
        return episode;
    }

    protected boolean episodeExistsForObservation(List<FeedEpisode> eventEpisodes, NormalizedObservation observation) {
        return eventEpisodes
                .stream()
                .anyMatch(episode -> episode.getObservations().contains(observation.getObservationId()));
    }

    private FeatureCollection computeEpisodeGeometries(Set<NormalizedObservation> episodeObservations) {
        List<Feature> features = new ArrayList<>();
        episodeObservations.stream()
                .filter(obs -> HP_SRV_SEARCH_PROVIDER.equalsIgnoreCase(obs.getProvider())
                        || (PDC_SQS_PROVIDER.equalsIgnoreCase(obs.getProvider())
                        && obs.getGeometries() != null && !Arrays.isNullOrEmpty(obs.getGeometries().getFeatures())
                        && obs.getGeometries().getFeatures()[0].getGeometry() != null
                        && "Point".equalsIgnoreCase(obs.getGeometries().getFeatures()[0].getGeometry().getType())))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .ifPresent(features::add);
        episodeObservations.stream()
                .filter(obs -> HP_SRV_MAG_PROVIDER.equalsIgnoreCase(obs.getProvider())
                        || (PDC_SQS_PROVIDER.equalsIgnoreCase(obs.getProvider())
                        && obs.getGeometries() != null && !Arrays.isNullOrEmpty(obs.getGeometries().getFeatures())
                        && obs.getGeometries().getFeatures()[0].getGeometry() != null
                        && ("Polygon".equalsIgnoreCase(obs.getGeometries().getFeatures()[0].getGeometry().getType())
                        || "MultiPolygon".equalsIgnoreCase(obs.getGeometries().getFeatures()[0].getGeometry().getType()))))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .ifPresent(features::add);
        episodeObservations.stream()
                .filter(obs -> PDC_MAP_SRV_PROVIDER.equalsIgnoreCase(obs.getProvider()))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .ifPresent(features::add);

        return new FeatureCollection(features.toArray(new Feature[0]));
    }

    protected Set<NormalizedObservation> findObservationsForEpisode(Set<NormalizedObservation> eventObservations,
                                                                    OffsetDateTime sourceUpdatedAt) {
        Set<NormalizedObservation> foundEvents = new HashSet<>();
        Duration timeRange = Duration.ofSeconds(TIME_RANGE_IN_SEC);
        AtomicReference<OffsetDateTime> currentTime = new AtomicReference<>(sourceUpdatedAt);

        foundEvents.addAll(eventObservations.stream()
                .sorted(Collections.reverseOrder(comparing(NormalizedObservation::getSourceUpdatedAt)))
                .dropWhile(obs -> obs.getSourceUpdatedAt().isAfter(sourceUpdatedAt)
                        || obs.getSourceUpdatedAt().isEqual(sourceUpdatedAt))
                .filter(obs -> {
                    if (Duration.between(obs.getSourceUpdatedAt(), currentTime.get()).minus(timeRange).isNegative()) {
                        currentTime.set(obs.getSourceUpdatedAt());
                        return true;
                    }
                    return false;
                }).collect(Collectors.toSet()));
        foundEvents.addAll(eventObservations.stream()
                .sorted(comparing(NormalizedObservation::getSourceUpdatedAt))
                .dropWhile(obs -> obs.getSourceUpdatedAt().isBefore(sourceUpdatedAt))
                .filter(obs -> {
                    if (Duration.between(currentTime.get(), obs.getSourceUpdatedAt()).minus(timeRange).isNegative()) {
                        currentTime.set(obs.getSourceUpdatedAt());
                        return true;
                    }
                    return false;
                }).collect(Collectors.toSet()));
        return foundEvents;
    }
}
