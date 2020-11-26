package io.kontur.eventapi.firms.episodecomposition;

import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.firms.FirmsUtil;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static io.kontur.eventapi.util.JsonUtil.readJson;
import static java.util.Arrays.asList;

@Component
public class FirmsEpisodeCombinator extends EpisodeCombinator {
    private final NormalizedObservationsDao observationsDao;

    private final GeoJSONReader geoJSONReader = new GeoJSONReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    public FirmsEpisodeCombinator(NormalizedObservationsDao observationsDao) {
        this.observationsDao = observationsDao;
    }

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return FirmsUtil.FIRMS_PROVIDERS.contains(observation.getProvider());
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData) {
        Set<FeedEpisode> episodesWithSameDate = feedData.getEpisodes().stream()
                .filter(e -> readObservations(e.getObservations())
                        .stream()
                        .anyMatch(o -> o.getSourceUpdatedAt().equals(observation.getSourceUpdatedAt())))
                .collect(Collectors.toSet());

        FeedEpisode feedEpisode = getOnlyElement(episodesWithSameDate, new FeedEpisode());
        populateMissedFields(feedEpisode, observation, feedData);

        return episodesWithSameDate.isEmpty() ? Optional.of(feedEpisode) : Optional.empty();
    }

    private void populateMissedFields(FeedEpisode feedEpisode, NormalizedObservation observation, FeedData feedData) {
        feedEpisode.setDescription(firstNonNull(feedEpisode.getDescription(), observation.getEpisodeDescription()));
        feedEpisode.setType(firstNonNull(feedEpisode.getType(), observation.getType()));
        feedEpisode.setActive(firstNonNull(feedEpisode.getActive(), observation.getActive()));
        feedEpisode.setSeverity(firstNonNull(feedEpisode.getSeverity(), observation.getEventSeverity()));
        feedEpisode.setStartedAt(firstNonNull(feedEpisode.getStartedAt(), observation.getStartedAt()));
        feedEpisode.setEndedAt(firstNonNull(feedEpisode.getEndedAt(), observation.getEndedAt()));
        feedEpisode.setUpdatedAt(firstNonNull(feedEpisode.getUpdatedAt(), observation.getLoadedAt()));
        feedEpisode.setSourceUpdatedAt(firstNonNull(feedEpisode.getSourceUpdatedAt(), observation.getSourceUpdatedAt()));

        feedEpisode.setGeometries(firstNonNull(feedEpisode.getGeometries(), () -> calculateGeometry(observation, feedData)));
        feedEpisode.setName(firstNonNull(feedEpisode.getName(), () -> calculateName(feedEpisode, feedData)));

        feedEpisode.addObservation(observation.getObservationId());
    }

    private FeatureCollection calculateGeometry(NormalizedObservation observation, FeedData feedData) {
        List<NormalizedObservation> observations = readObservations(feedData.getObservations());

        OffsetDateTime oneDayBeforeObservation = observation.getSourceUpdatedAt().minus(24, ChronoUnit.HOURS);

        Geometry geometry = observations.stream()
                .filter(e -> e.getSourceUpdatedAt().isAfter(oneDayBeforeObservation) || e.getSourceUpdatedAt().isEqual(oneDayBeforeObservation))
                .filter(e -> e.getSourceUpdatedAt().isBefore(observation.getSourceUpdatedAt()) || e.getSourceUpdatedAt().isEqual(observation.getSourceUpdatedAt()))
                .map(normalizedObservation -> toGeometry(normalizedObservation.getGeometries()))
                .distinct()
                .reduce(Geometry::union)
                .get();

        return createFirmGeometry(geometry, getFirmFeature(observation.getGeometries()).getProperties());
    }

    private String calculateName(FeedEpisode feedEpisode, FeedData feedData) {
        List<NormalizedObservation> observations = readObservations(feedData.getObservations());
        observations.sort(Comparator.comparing(NormalizedObservation::getSourceUpdatedAt));
        long burningTime = observations.get(0).getSourceUpdatedAt().until(feedEpisode.getSourceUpdatedAt(), ChronoUnit.HOURS);
        FeatureCollection geometries = feedEpisode.getGeometries();
        String burntArea = String.format("%.7f", toGeometry(geometries).getArea());
        return "Burnt area " + burntArea + (burningTime > 0 ? ", Burning time " + burningTime + "h" : "");
    }

    private Geometry toGeometry(FeatureCollection geometries) {
        return toGeometry(getFirmFeature(geometries));
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

    private List<NormalizedObservation> readObservations(List<UUID> observations) {
        return observationsDao.getObservations(observations);
    }

    private FeatureCollection createFirmGeometry(Geometry geometry, Map<String, Object> properties) {
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