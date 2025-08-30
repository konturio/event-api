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
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.FeatureCollection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class EpisodeCombinator implements Applicable<NormalizedObservation> {

    protected static final Integer COMMON_GEOMETRY_FUNCTION = 0;
    protected static final Integer NHC_GEOMETRY_FUNCTION = 1;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        return postProcessEpisodes(episodes, Duration.ZERO);
    }

    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes, Duration mergeTolerance) {
        if (CollectionUtils.isEmpty(episodes) || episodes.size() == 1) {
            return episodes;
        }

        List<FeedEpisode> sorted = new ArrayList<>(episodes);
        sorted.sort(
                comparing(FeedEpisode::getStartedAt,
                        java.util.Comparator.nullsLast(java.time.OffsetDateTime::compareTo))
                        .thenComparing(FeedEpisode::getEndedAt,
                                java.util.Comparator.nullsLast(java.time.OffsetDateTime::compareTo))
        );

        List<FeedEpisode> merged = new ArrayList<>();
        FeedEpisode current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            FeedEpisode next = sorted.get(i);
            boolean timeAdjacent = mergeTolerance == null
                    || (current.getEndedAt() != null && next.getStartedAt() != null
                    && !next.getStartedAt().isAfter(current.getEndedAt().plus(mergeTolerance)));
            if (timeAdjacent && canMerge(current, next)) {
                if (next.getStartedAt() != null
                        && (current.getStartedAt() == null || next.getStartedAt().isBefore(current.getStartedAt()))) {
                    current.setStartedAt(next.getStartedAt());
                }
                if (next.getEndedAt() != null
                        && (current.getEndedAt() == null || next.getEndedAt().isAfter(current.getEndedAt()))) {
                    current.setEndedAt(next.getEndedAt());
                }
                if (next.getUpdatedAt() != null
                        && (current.getUpdatedAt() == null || next.getUpdatedAt().isAfter(current.getUpdatedAt()))) {
                    current.setUpdatedAt(next.getUpdatedAt());
                }
                if (next.getSourceUpdatedAt() != null
                        && (current.getSourceUpdatedAt() == null
                        || next.getSourceUpdatedAt().isAfter(current.getSourceUpdatedAt()))) {
                    current.setSourceUpdatedAt(next.getSourceUpdatedAt());
                }
                current.addObservations(next.getObservations());
                current.addUrlIfNotExists(next.getUrls());
                if (next.getLoss() != null) {
                    if (current.getLoss() == null) {
                        current.setLoss(new HashMap<>());
                    }
                    current.getLoss().putAll(next.getLoss());
                }
                if (next.getSeverityData() != null) {
                    if (current.getSeverityData() == null) {
                        current.setSeverityData(new HashMap<>());
                    }
                    current.getSeverityData().putAll(next.getSeverityData());
                }
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private boolean canMerge(FeedEpisode a, FeedEpisode b) {
        return Objects.equals(a.getType(), b.getType())
                && Objects.equals(a.getSeverity(), b.getSeverity())
                && Objects.equals(a.getName(), b.getName())
                && Objects.equals(a.getProperName(), b.getProperName())
                && Objects.equals(a.getDescription(), b.getDescription())
                && Objects.equals(a.getLocation(), b.getLocation())
                && Objects.equals(a.getLoss(), b.getLoss())
                && Objects.equals(a.getSeverityData(), b.getSeverityData())
                && urlsEqualIgnoringOrder(a.getUrls(), b.getUrls())
                && geometriesEqual(a.getGeometries(), b.getGeometries());
    }

    private boolean urlsEqualIgnoringOrder(List<String> a, List<String> b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        List<String> sortedA = new ArrayList<>(a);
        List<String> sortedB = new ArrayList<>(b);
        Collections.sort(sortedA);
        Collections.sort(sortedB);
        return sortedA.equals(sortedB);
    }

    private boolean geometriesEqual(FeatureCollection a, FeatureCollection b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        try {
            return MAPPER.writeValueAsString(a).equals(MAPPER.writeValueAsString(b));
        } catch (JsonProcessingException e) {
            return false;
        }
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
        // sort by observation date to ensure deterministic order for comparison
        return observations
                .stream()
                .sorted(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(NormalizedObservation::getObservationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
