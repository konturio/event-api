package io.kontur.eventapi.episodecomposition;

import static io.kontur.eventapi.entity.Severity.UNKNOWN;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.job.Applicable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public abstract class EpisodeCombinator implements Applicable<NormalizedObservation> {

    protected static final Integer COMMON_GEOMETRY_FUNCTION = 0;
    protected static final Integer NHC_GEOMETRY_FUNCTION = 1;
    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        return episodes;
    }

    public abstract List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations);

    protected FeedEpisode createDefaultEpisode(NormalizedObservation observation) {
        FeedEpisode feedEpisode = new FeedEpisode();
        feedEpisode.setName(observation.getName());
        feedEpisode.setDescription(observation.getEpisodeDescription());
        feedEpisode.setType(observation.getType());
        feedEpisode.setSeverity(observation.getEventSeverity());
        feedEpisode.setStartedAt(observation.getStartedAt());
        feedEpisode.setEndedAt(observation.getEndedAt());
        feedEpisode.setUpdatedAt(observation.getLoadedAt());
        feedEpisode.setSourceUpdatedAt(observation.getSourceUpdatedAt());
        feedEpisode.addObservation(observation.getObservationId());
        if (!CollectionUtils.isEmpty(observation.getUrls())) {
            feedEpisode.addUrlIfNotExists(observation.getUrls());
        }
        feedEpisode.setProperName(observation.getProperName());
        feedEpisode.setLocation(observation.getRegion());
        feedEpisode.setLoss(observation.getLoss());
        feedEpisode.setSeverityData(observation.getSeverityData());

        if (observation.getGeometries() != null) {
            feedEpisode.setGeometries(observation.getGeometries());
        }

        return feedEpisode;
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

    protected String findEpisodeName(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .filter(obs -> isNotBlank(obs.getName()))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(NormalizedObservation::getName)
                .orElse(null);
    }

    protected String findEpisodeDescription(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .filter(obs -> isNotBlank(obs.getDescription()))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(NormalizedObservation::getDescription)
                .orElse(null);
    }

    protected OffsetDateTime findEpisodeStartedAt(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getStartedAt)
                .filter(Objects::nonNull)
                .min(OffsetDateTime::compareTo)
                .orElse(null);
    }

    protected OffsetDateTime findEpisodeEndedAt(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getEndedAt)
                .filter(Objects::nonNull)
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

    protected Map<String, Object> findEpisodeLoss(Set<NormalizedObservation> episodeObservations) {
        Map<String, Object> loss = new HashMap<>();
        episodeObservations
                .stream()
                .sorted(comparing(NormalizedObservation::getSourceUpdatedAt))
                .forEachOrdered(obs -> obs.getLoss().entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .forEach(e -> loss.put(e.getKey(), e.getValue())));
        return loss;
    }

    protected Severity findEpisodeSeverity(Set<NormalizedObservation> observations) {
        return observations.stream()
                .map(NormalizedObservation::getEventSeverity)
                .filter(Objects::nonNull)
                .max(comparing(Severity::getValue))
                .orElse(UNKNOWN);
    }

    protected String findEpisodeLocation(Set<NormalizedObservation> observations) {
        return observations.stream()
                .filter(obs -> isNotBlank(obs.getRegion()))
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(NormalizedObservation::getRegion)
                .orElse(null);
    }

    protected List<String> findEpisodeUrls(Set<NormalizedObservation> observations) {
        return observations.stream()
                .map(NormalizedObservation::getUrls)
                .filter(urls -> urls != null && !urls.isEmpty())
                .flatMap(List::stream)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(toList());
    }
}
