package io.kontur.eventapi.firms.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.firms.FirmsUtil;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static io.kontur.eventapi.util.JsonUtil.readJson;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Component
public class FirmsEpisodeCombinator extends EpisodeCombinator {
    private final GeoJSONReader geoJSONReader = new GeoJSONReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return FirmsUtil.FIRMS_PROVIDERS.contains(observation.getProvider());
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        Set<FeedEpisode> existingEpisodeForObservation = feedData.getEpisodes()
                .stream()
                .filter(ep -> getLatestObservationInEpisode(ep, eventObservations)
                        .filter(ob -> ob.getSourceUpdatedAt().equals(observation.getSourceUpdatedAt()))
                        .isPresent()
                )
                .collect(Collectors.toSet());

        FeedEpisode feedEpisode = getOnlyElement(existingEpisodeForObservation, new FeedEpisode());
        populateMissedFields(feedEpisode, observation, feedData, eventObservations);

        return existingEpisodeForObservation.isEmpty() ? Optional.of(feedEpisode) : Optional.empty();
    }

    private Optional<NormalizedObservation> getLatestObservationInEpisode(FeedEpisode episode, Set<NormalizedObservation> eventObservations) {
        return readObservations(episode.getObservations(), eventObservations)
                .stream()
                .max(comparing(NormalizedObservation::getSourceUpdatedAt));
    }

    private void populateMissedFields(FeedEpisode episode, NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        episode.setDescription(firstNonNull(episode.getDescription(), observation.getEpisodeDescription()));
        episode.setType(firstNonNull(episode.getType(), observation.getType()));
        episode.setActive(firstNonNull(episode.getActive(), observation.getActive()));
        episode.setSeverity(firstNonNull(episode.getSeverity(), observation.getEventSeverity()));
        episode.setStartedAt(firstNonNull(episode.getStartedAt(), observation.getStartedAt()));
        episode.setEndedAt(firstNonNull(episode.getEndedAt(), calculateEndedDate(observation, eventObservations)));
        episode.setUpdatedAt(firstNonNull(episode.getUpdatedAt(), observation.getLoadedAt()));

        if (episode.getObservations().isEmpty()) {
            List<NormalizedObservation> feedObservations = readObservations(feedData.getObservations(), eventObservations);
            List<NormalizedObservation> episodeObservations = findObservationsForEpisode(observation, feedObservations);
            List<UUID> episodeObservationsIds = episodeObservations.stream().map(NormalizedObservation::getObservationId).collect(toList());

            episode.getObservations().addAll(episodeObservationsIds);
        }

        episode.setGeometries(firstNonNull(episode.getGeometries(), () -> calculateGeometry(episode, observation, eventObservations)));
        episode.setSourceUpdatedAt(firstNonNull(episode.getSourceUpdatedAt(), observation.getSourceUpdatedAt()));
        episode.setName(firstNonNull(episode.getName(), () -> calculateName(episode, feedData, eventObservations)));
    }

    private OffsetDateTime calculateEndedDate(NormalizedObservation observation, Set<NormalizedObservation> eventObservations) {
        return eventObservations
                .stream()
                .map(NormalizedObservation::getStartedAt)
                .filter(startDate -> startDate.isAfter(observation.getStartedAt()))
                .min(OffsetDateTime::compareTo)
                .orElse(observation.getStartedAt().plusHours(24));
    }

    private FeatureCollection calculateGeometry(FeedEpisode episode, NormalizedObservation observation, Set<NormalizedObservation> eventObservations) {
        Geometry geometry = readObservations(episode.getObservations(), eventObservations)
                .stream()
                .map(normalizedObservation -> toGeometry(normalizedObservation.getGeometries()))
                .distinct()
                .reduce(Geometry::union)
                .get();

        return createFeatureCollection(geometry, getFirmFeature(observation.getGeometries()).getProperties());
    }

    private List<NormalizedObservation> findObservationsForEpisode(NormalizedObservation observation, List<NormalizedObservation> observations) {
        OffsetDateTime oneDayBeforeObservation = observation.getSourceUpdatedAt().minus(24, ChronoUnit.HOURS);

        return observations.stream()
                .filter(o -> o.getSourceUpdatedAt().isAfter(oneDayBeforeObservation) || o.getSourceUpdatedAt().isEqual(oneDayBeforeObservation))
                .filter(o -> o.getSourceUpdatedAt().isBefore(observation.getSourceUpdatedAt()) || o.getSourceUpdatedAt().isEqual(observation.getSourceUpdatedAt()))
                .collect(toList());
    }

    private String calculateName(FeedEpisode feedEpisode, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        List<NormalizedObservation> observations = readObservations(feedData.getObservations(), eventObservations);
        observations.sort(comparing(NormalizedObservation::getSourceUpdatedAt));
        long burningTime = observations.get(0).getSourceUpdatedAt().until(feedEpisode.getSourceUpdatedAt(), ChronoUnit.HOURS);
        String burntArea = getArea(feedEpisode, eventObservations);
        return "Burnt area " + burntArea + "km" + (burningTime > 0 ? ", Burning time " + burningTime + "h" : "");
    }

    private String getArea(FeedEpisode feedEpisode, Set<NormalizedObservation> eventObservations) {
        return readObservations(feedEpisode.getObservations(), eventObservations)
                .stream()
                .map(normalizedObservation -> toGeometry(normalizedObservation.getGeometries()))
                .distinct()
                .map(geometry -> {
                    PolygonArea polygonArea = new PolygonArea(Geodesic.WGS84, false);
                    Arrays.stream(geometry.getCoordinates()).forEach(c -> polygonArea.AddPoint(c.getY(), c.getX()));
                    double areaInMeters = Math.abs(polygonArea.Compute().area);
                    double areaInKm = areaInMeters / 1_000_000;
                    return areaInKm;
                })
                .reduce(Double::sum)
                .map(area -> String.format("%.3f", area))
                .get();
    }

    private Geometry toGeometry(String geometries) {
        return toGeometry(getFirmFeature(geometries));
    }

    private Geometry toGeometry(Feature firmFeature) {
        return geoJSONReader.read(firmFeature.getGeometry());
    }

    private Feature getFirmFeature(FeatureCollection featureCollection) {
        return getOnlyElement(asList(featureCollection.getFeatures()));
    }

    private Feature getFirmFeature(String geometries) {
        return getFirmFeature(getFeatureCollection(geometries));
    }

    private FeatureCollection getFeatureCollection(String geometries) {
        return readJson(geometries, FeatureCollection.class);
    }

    private List<NormalizedObservation> readObservations(List<UUID> observationsIds, Set<NormalizedObservation> eventObservations) {
        List<NormalizedObservation> observationsByIds = eventObservations
                .stream().filter(e -> observationsIds.contains(e.getObservationId()))
                .collect(toList());

        checkState(observationsByIds.size() == observationsIds.size(),
                "can not find all needed observations in event");

        return observationsByIds;
    }

    private FeatureCollection createFeatureCollection(Geometry geometry, Map<String, Object> properties) {
        org.wololo.geojson.Geometry write = geoJSONWriter.write(geometry);
        Feature feature = new Feature(write, properties);
        return new FeatureCollection(new Feature[]{feature});
    }

    public static <T> T firstNonNull(T first, Supplier<T> second) {
        if (first != null) {
            return first;
        } else if (second != null) {
            return second.get();
        } else {
            return null;
        }
    }

    public static <T> T firstNonNull(T first, T second) {
        return firstNonNull(first, () -> second);
    }
}