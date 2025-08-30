package io.kontur.eventapi.pdc.episodecomposition;

import static com.google.common.collect.Iterators.getLast;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.*;
import static io.kontur.eventapi.util.GeometryUtil.isEqualGeometries;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.time.Duration;
import java.util.*;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.bouncycastle.util.Arrays;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

public abstract class BasePdcEpisodeCombinator extends EpisodeCombinator {

    protected static final Duration TIME_RANGE_SEC = Duration.ofSeconds(90);

    @Override
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData,
                                                Set<NormalizedObservation> eventObservations) {
        if (!feedData.getEpisodes().isEmpty()) return emptyList();
        if (isOnlyPdcMapSrvObservations(eventObservations)) {
            return collectExposureEpisodes(eventObservations);
        }
        Map<Boolean, List<NormalizedObservation>> observationsByProvider = eventObservations.stream()
                .collect(partitioningBy(obs -> List.of(PDC_MAP_SRV_PROVIDER, PDC_MAP_SRV_NASA_PROVIDER).contains(obs.getProvider())));
        List<FeedEpisode> episodes = collectInitialEpisodes(observationsByProvider.getOrDefault(false, emptyList()));
        List<FeedEpisode> episodesWithoutDuplicates = mergeDuplicatedEpisodes(episodes);
        addExposuresToEpisodes(episodesWithoutDuplicates, new HashSet<>(observationsByProvider.getOrDefault(true, emptyList())));
        return episodesWithoutDuplicates;
    }

    private boolean isOnlyPdcMapSrvObservations(Set<NormalizedObservation> eventObservations) {
        return eventObservations.stream()
                .map(NormalizedObservation::getProvider)
                .allMatch(List.of(PDC_MAP_SRV_PROVIDER, PDC_MAP_SRV_NASA_PROVIDER)::contains);
    }

    private List<FeedEpisode> collectExposureEpisodes(Set<NormalizedObservation> observations) {
        return observations.stream()
                .sorted(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(obs -> {
                    FeedEpisode episode = createDefaultEpisode(obs);
                    episode.setStartedAt(obs.getSourceUpdatedAt());
                    episode.setEndedAt(obs.getSourceUpdatedAt());
                    return episode;
                })
                .toList();
    }

    protected List<FeedEpisode> collectInitialEpisodes(List<NormalizedObservation> observations) {
        List<FeedEpisode> episodes = new ArrayList<>();
        Set<NormalizedObservation> episodeObservations = new LinkedHashSet<>();
        observations.stream()
                .sorted(comparing(NormalizedObservation::getStartedAt).thenComparing(NormalizedObservation::getEndedAt))
                .forEachOrdered(obs -> {
                    if (!episodeObservations.isEmpty()
                            && exceedsRange(getLast(episodeObservations.iterator()), obs)) {
                        episodes.add(computeEpisode(episodeObservations, getLast(episodes.iterator(), null)));
                        episodeObservations.clear();
                    }
                    episodeObservations.add(obs);
                });
        if (!episodeObservations.isEmpty()) {
            episodes.add(computeEpisode(episodeObservations, getLast(episodes.iterator(), null)));
        }
        return episodes;
    }

    private Boolean exceedsRange(NormalizedObservation observation1, NormalizedObservation observation2) {
        return !Duration.between(observation1.getEndedAt(), observation2.getEndedAt())
                .minus(TIME_RANGE_SEC)
                .isNegative();
    }

    private FeedEpisode computeEpisode(Set<NormalizedObservation> episodeObservations, FeedEpisode previousEpisode) {
        FeedEpisode episode = createDefaultEpisode(findLatestEpisodeObservation(episodeObservations));
        episode.setName(findEpisodeName(episodeObservations));
        episode.setDescription(findEpisodeDescription(episodeObservations));
        episode.setSeverity(findEpisodeSeverity(episodeObservations));
        episode.setStartedAt(previousEpisode == null ? findEpisodeStartedAt(episodeObservations) : previousEpisode.getEndedAt());
        episode.setEndedAt(findEpisodeEndedAt(episodeObservations));
        episode.setUpdatedAt(findEpisodeUpdatedAt(episodeObservations));
        episode.setLocation(findEpisodeLocation(episodeObservations));
        episode.setLoss(findEpisodeLoss(episodeObservations));
        episode.setObservations(mapObservationsToIDs(episodeObservations));
        episode.setGeometries(computeEpisodeGeometries(episodeObservations));
        episode.setUrls(findEpisodeUrls(episodeObservations));
        return episode;
    }

    private List<FeedEpisode> mergeDuplicatedEpisodes(List<FeedEpisode> episodes) {
        if (episodes.size() < 2) return episodes;
        List<FeedEpisode> episodesWithoutDuplicates = new ArrayList<>();
        episodes.stream()
                .sorted(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getEndedAt))
                .forEachOrdered(episode -> {
                    FeedEpisode lastEpisode = getLast(episodesWithoutDuplicates.iterator(), null);
                    if (lastEpisode == null || !sameEpisodes(lastEpisode, episode)) {
                        episodesWithoutDuplicates.add(episode);
                    } else {
                        lastEpisode.setEndedAt(episode.getEndedAt());
                        lastEpisode.setSourceUpdatedAt(episode.getSourceUpdatedAt());
                        lastEpisode.setUpdatedAt(episode.getUpdatedAt());
                        lastEpisode.addObservations(episode.getObservations());
                        lastEpisode.addUrlIfNotExists(episode.getUrls());
                    }
                });
        return episodesWithoutDuplicates;
    }

    private boolean sameEpisodes(FeedEpisode episode1, FeedEpisode episode2) {
        FeatureCollection geom1 = episode1.getGeometries();
        FeatureCollection geom2 = episode2.getGeometries();
        boolean geometriesEqual = (geom1 == null && geom2 == null)
                || (geom1 != null && geom2 != null && isEqualGeometries(geom1, geom2));

        return equalsIgnoreCase(episode1.getName(), episode2.getName())
                && Objects.equals(episode1.getLoss(), episode2.getLoss())
                && episode1.getSeverity() == episode2.getSeverity()
                && equalsIgnoreCase(episode1.getLocation(), episode2.getLocation())
                && geometriesEqual;
    }

    private void addExposuresToEpisodes(List<FeedEpisode> episodes, Set<NormalizedObservation> exposureObservations) {
        for (int i = 0; i < episodes.size(); i++) {
            FeedEpisode episode = episodes.get(i);
            List<NormalizedObservation> episodeExposures = exposureObservations.stream()
                    .filter(obs -> (obs.getSourceUpdatedAt().isEqual(episode.getStartedAt())
                            || obs.getSourceUpdatedAt().isAfter(episode.getStartedAt()))
                            && (obs.getSourceUpdatedAt().isEqual(episode.getEndedAt())
                            || obs.getSourceUpdatedAt().isBefore(episode.getEndedAt())))
                    .collect(toList());
            if (i == 0) {
                episodeExposures.addAll(exposureObservations.stream()
                        .filter(obs -> (obs.getSourceUpdatedAt().isBefore(episode.getStartedAt())))
                        .toList());
            }
            if (i == episodes.size() - 1) {
                episodeExposures.addAll(exposureObservations.stream()
                        .filter(obs -> (obs.getSourceUpdatedAt().isAfter(episode.getEndedAt())))
                        .toList());
            }
            if (episodeExposures.isEmpty()) {
                exposureObservations.stream()
                        .filter(obs -> (obs.getSourceUpdatedAt().isBefore(episode.getStartedAt())))
                        .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                        .ifPresent(episodeExposures::add);
            }
            addExposuresToEpisode(episode, episodeExposures);
        }
    }

    private void addExposuresToEpisode(FeedEpisode episode, List<NormalizedObservation> exposureObservations) {
        exposureObservations.stream()
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .ifPresent(obs -> {
                    Feature[] features = addAll(episode.getGeometries().getFeatures(), obs.getGeometries().getFeatures());
                    episode.setGeometries(new FeatureCollection(features));
                });
    }

    private FeatureCollection computeEpisodeGeometries(Set<NormalizedObservation> episodeObservations) {
        List<Feature> features = new ArrayList<>();
        episodeObservations.stream()
                .filter(obs -> HP_SRV_SEARCH_PROVIDER.equalsIgnoreCase(obs.getProvider())
                        || (List.of(PDC_SQS_PROVIDER, PDC_SQS_NASA_PROVIDER).contains(obs.getProvider())
                        && obs.getGeometries() != null && !Arrays.isNullOrEmpty(obs.getGeometries().getFeatures())
                        && obs.getGeometries().getFeatures()[0].getGeometry() != null
                        && "Point".equalsIgnoreCase(obs.getGeometries().getFeatures()[0].getGeometry().getType())))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .ifPresent(features::add);
        episodeObservations.stream()
                .filter(obs -> HP_SRV_MAG_PROVIDER.equalsIgnoreCase(obs.getProvider())
                        || (List.of(PDC_SQS_PROVIDER, PDC_SQS_NASA_PROVIDER).contains(obs.getProvider())
                        && obs.getGeometries() != null && !Arrays.isNullOrEmpty(obs.getGeometries().getFeatures())
                        && obs.getGeometries().getFeatures()[0].getGeometry() != null
                        && ("Polygon".equalsIgnoreCase(obs.getGeometries().getFeatures()[0].getGeometry().getType())
                        || "MultiPolygon".equalsIgnoreCase(obs.getGeometries().getFeatures()[0].getGeometry().getType()))))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(observation -> observation.getGeometries().getFeatures()[0])
                .ifPresent(features::add);
        return new FeatureCollection(features.toArray(new Feature[0]));
    }
}
