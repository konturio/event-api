package io.kontur.eventapi.episodecomposition;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.job.Applicable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class EpisodeCombinator implements Applicable<NormalizedObservation> {

    private final static Logger LOG = LoggerFactory.getLogger(EpisodeCombinator.class);

    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        if (CollectionUtils.isEmpty(episodes) || episodes.size() == 1) {
            return episodes;
        }
        episodes.sort(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getSourceUpdatedAt));
        for (int i = 0; i < episodes.size() - 1; i++) {
            FeedEpisode currentEpisode = episodes.get(i);
            FeedEpisode nextEpisode = episodes.get(i + 1);
            if (!currentEpisode.getSourceUpdatedAt().isAfter(currentEpisode.getEndedAt())) {
                if (currentEpisode.getSourceUpdatedAt().isAfter(currentEpisode.getStartedAt())) {
                    currentEpisode.setEndedAt(currentEpisode.getSourceUpdatedAt());
                } else {
                    LOG.debug("SourceUpdatedAt is before StartedAt for observations: {}",
                            String.join(",", currentEpisode.getObservations().stream().map(UUID::toString).toList()));
                }
            }
            if (nextEpisode.getSourceUpdatedAt().isBefore(currentEpisode.getEndedAt())) {
                currentEpisode.setEndedAt(nextEpisode.getSourceUpdatedAt());
                nextEpisode.setStartedAt(nextEpisode.getSourceUpdatedAt());
            } else {
                nextEpisode.setStartedAt(currentEpisode.getEndedAt());
            }
        }
        return episodes;
    }

    public abstract Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations);

    protected Optional<FeedEpisode> createDefaultEpisode(NormalizedObservation observation) {
        FeedEpisode feedEpisode = new FeedEpisode();
        feedEpisode.setName(observation.getName());
        feedEpisode.setDescription(observation.getEpisodeDescription());
        feedEpisode.setType(observation.getType());
        feedEpisode.setActive(observation.getActive());
        feedEpisode.setSeverity(observation.getEventSeverity());
        feedEpisode.setStartedAt(observation.getStartedAt());
        feedEpisode.setEndedAt(observation.getEndedAt());
        feedEpisode.setUpdatedAt(observation.getLoadedAt());
        feedEpisode.setSourceUpdatedAt(observation.getSourceUpdatedAt());
        feedEpisode.addObservation(observation.getObservationId());
        feedEpisode.addUrlIfNotExists(observation.getSourceUri());
        feedEpisode.setProperName(observation.getProperName());
        feedEpisode.setLocation(observation.getRegion());

        if (observation.getGeometries() != null) {
            feedEpisode.setGeometries(observation.getGeometries());
        }

        return Optional.of(feedEpisode);
    }

    protected boolean episodeExistsForObservation(List<FeedEpisode> eventEpisodes, NormalizedObservation observation) {
        return eventEpisodes
                .stream()
                .anyMatch(episode -> episode.getSourceUpdatedAt().equals(observation.getSourceUpdatedAt()));
    }

    protected Set<NormalizedObservation> findObservationsForEpisode(Set<NormalizedObservation> eventObservations,
                                                                    OffsetDateTime sourceUpdateAt,
                                                                    long timeRangeMinus, long timeRangePlus) {
        return findObservationsForEpisode(eventObservations, sourceUpdateAt, timeRangeMinus, timeRangePlus,
                ChronoUnit.SECONDS);
    }

    protected Set<NormalizedObservation> findObservationsForEpisode(Set<NormalizedObservation> eventObservations,
                                                                    OffsetDateTime sourceUpdateAt,
                                                                    long timeRangeMinus, long timeRangePlus,
                                                                    ChronoUnit timeUnit) {
        if (timeRangeMinus > 0 || timeRangePlus > 0) {
            OffsetDateTime startOfRange = sourceUpdateAt.minus(timeRangeMinus, timeUnit);
            OffsetDateTime endOfRange = sourceUpdateAt.plus(timeRangePlus, timeUnit);
            return eventObservations.stream()
                    .filter(observation -> observation.getSourceUpdatedAt().isAfter(startOfRange)
                            || observation.getSourceUpdatedAt().isEqual(startOfRange))
                    .filter(observation -> observation.getSourceUpdatedAt().isBefore(endOfRange)
                            || observation.getSourceUpdatedAt().isEqual(endOfRange))
                    .collect(toSet());
        } else {
            return eventObservations.stream()
                    .filter(o -> o.getSourceUpdatedAt().isEqual(sourceUpdateAt))
                    .collect(toSet());
        }
    }

    protected NormalizedObservation findLatestEpisodeObservation(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .orElse(null);
    }

    protected Optional<NormalizedObservation> findLatestEpisodeObservationWithName(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .filter(obs -> StringUtils.isNotBlank(obs.getName()))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt));
    }

    protected Optional<NormalizedObservation> findLatestEpisodeObservationWithDescription(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .filter(obs -> StringUtils.isNotBlank(obs.getDescription()))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt));
    }

    protected OffsetDateTime findEpisodeStartedAt(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getStartedAt)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
    }

    protected OffsetDateTime findEpisodeEndedAt(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getEndedAt)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
    }

    protected OffsetDateTime findEpisodeUpdatedAt(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getLoadedAt)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
    }

    protected Set<UUID> mapObservationsToIDs(Set<NormalizedObservation> observations) {
        return observations
                .stream()
                .map(NormalizedObservation::getObservationId)
                .collect(toSet());
    }

}
