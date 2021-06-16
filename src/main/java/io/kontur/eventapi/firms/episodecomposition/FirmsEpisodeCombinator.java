package io.kontur.eventapi.firms.episodecomposition;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.firms.FirmsUtil;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import org.apache.commons.lang3.math.NumberUtils;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(10000));
    private final KonturApiClient konturApiClient;
    private final H3Core h3;

    public FirmsEpisodeCombinator(KonturApiClient konturApiClient) {
        this.konturApiClient = konturApiClient;
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException("failed to create h3 engine", e);
        }
    }

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
        episode.setStartedAt(firstNonNull(episode.getStartedAt(), observation.getStartedAt()));
        episode.setEndedAt(firstNonNull(episode.getEndedAt(), calculateEndedDate(observation, eventObservations)));

        List<NormalizedObservation> feedObservations = readObservations(feedData.getObservations(), eventObservations);
        List<NormalizedObservation> episodeObservations;
        if (episode.getObservations().isEmpty()) {
            episodeObservations = findObservationsForEpisode(observation, feedObservations);
            List<UUID> episodeObservationsIds = episodeObservations.stream().map(NormalizedObservation::getObservationId).collect(toList());

            episode.getObservations().addAll(episodeObservationsIds);
        } else {
            episodeObservations = readObservations(episode.getObservations(), eventObservations);
        }

        episode.setUpdatedAt(calculateUpdatedDate(episodeObservations));
        Geometry episodeGeometry = calculateGeometry(episodeObservations);
        episode.setGeometries(firstNonNull(episode.getGeometries(), () -> createEpisodeGeometryFeatureCollection(observation, episodeGeometry)));
        episode.setSourceUpdatedAt(firstNonNull(episode.getSourceUpdatedAt(), observation.getSourceUpdatedAt()));

        feedObservations.sort(comparing(NormalizedObservation::getStartedAt));
        Double area = calculateBurntAreaUpToCurrentObservation(observation, feedObservations);
        long burningTime = feedObservations.get(0).getStartedAt().until(episode.getEndedAt(), ChronoUnit.HOURS);

        episode.setSeverity(calculateSeverity(area, burningTime));
        episode.setName(calculateName(episodeObservations, area, burningTime));
    }

    private OffsetDateTime calculateEndedDate(NormalizedObservation observation, Set<NormalizedObservation> eventObservations) {
        return eventObservations
                .stream()
                .map(NormalizedObservation::getStartedAt)
                .filter(startDate -> startDate.isAfter(observation.getStartedAt()))
                .min(OffsetDateTime::compareTo)
                .orElse(observation.getStartedAt().plusHours(24));
    }

    private OffsetDateTime calculateUpdatedDate(List<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getLoadedAt)
                .max(OffsetDateTime::compareTo)
                .get();
    }

    private Geometry calculateGeometry(List<NormalizedObservation> observations) {
        return observations
                .stream()
                .map(normalizedObservation -> toGeometry(normalizedObservation.getGeometries()))
                .distinct()
                .reduce(Geometry::union)
                .get();
    }

    private FeatureCollection createEpisodeGeometryFeatureCollection(NormalizedObservation observation, Geometry geometry) {
        return createFeatureCollection(geometry, getFirmFeature(observation.getGeometries()).getProperties());
    }

    private List<NormalizedObservation> findObservationsForEpisode(NormalizedObservation observation, List<NormalizedObservation> observations) {
        OffsetDateTime oneDayBeforeObservation = observation.getSourceUpdatedAt().minus(24, ChronoUnit.HOURS);

        return observations.stream()
                .filter(o -> o.getSourceUpdatedAt().isAfter(oneDayBeforeObservation) || o.getSourceUpdatedAt().isEqual(oneDayBeforeObservation))
                .filter(o -> o.getSourceUpdatedAt().isBefore(observation.getSourceUpdatedAt()) || o.getSourceUpdatedAt().isEqual(observation.getSourceUpdatedAt()))
                .collect(toList());
    }

    private Severity calculateSeverity(Double area, long burningTime) {
        if (burningTime <= 24) {
            return Severity.MINOR;
        }
        if (area == null) {
            return Severity.UNKNOWN;
        }
        if (area < 10) {
            return Severity.MINOR;
        } else if (area < 50) {
            return Severity.MODERATE;
        } else if (area < 100) {
            return Severity.SEVERE;
        }
        return Severity.EXTREME;
    }

    private String calculateName(List<NormalizedObservation> episodeObservations, Double area, long burningTime) {
        String burntArea = String.format(Locale.US, "%.3f", area);
        String areaName = getBurntAreaName(episodeObservations);
        if (!StringUtils.isEmpty(areaName)) {
            areaName = "Thermal anomaly in " + areaName + ". ";
        } else {
            areaName = "Thermal anomaly in an unknown area. ";
        }
        return areaName + "Burnt area " + burntArea + " km\u00B2" + (burningTime > 24 ? ", burning " + burningTime + " hours." : "");
    }

    private String getBurntAreaName(List<NormalizedObservation> episodeObservations) {
        Geometry centroid = calculateH3Centroid(calculateCentroid(episodeObservations));

        FeatureCollection adminBoundaries = konturApiClient.adminBoundaries(centroid.toText(), 10);
        if (adminBoundaries == null || adminBoundaries.getFeatures() == null) {
            return "";
        }
        return Stream.of(adminBoundaries.getFeatures())
                .map(Feature::getProperties)
                .filter(prop -> NumberUtils.isCreatable(String.valueOf(prop.get("admin_level"))))
                .sorted(Comparator.comparing(prop -> Double.parseDouble(String.valueOf(prop.get("admin_level")))))
                .limit(3)
                .map(prop -> ((Map<String, String>)prop.get("tags")))
                .map(tags -> {
                    if (tags.containsKey("int_name")) {
                        return tags.get("int_name");
                    } else if (tags.containsKey("name:en")) {
                        return tags.get("name:en");
                    } else {
                        return tags.get("name");
                    }
                })
                .collect(Collectors.joining(", "));
    }

    private Point calculateCentroid(List<NormalizedObservation> episodeObservations) {
        List<Geometry> geometries = episodeObservations
                .stream()
                .map(no -> toGeometry(no.getGeometries()))
                .collect(toList());
        return geometryFactory.buildGeometry(geometries).getCentroid();
    }

    private Point calculateH3Centroid(Point geometry) {
        GeoCoord geoCoord = h3.h3ToGeo(h3.geoToH3(geometry.getY(), geometry.getX(), 8));
        return geometryFactory.createPoint(new Coordinate(geoCoord.lng, geoCoord.lat));
    }

    private Double calculateBurntAreaUpToCurrentObservation(NormalizedObservation observation,
                                                            List<NormalizedObservation> feedObservations) {
        List<NormalizedObservation> previousObservations = feedObservations.stream()
                .filter(o -> o.getSourceUpdatedAt().isBefore(observation.getSourceUpdatedAt())
                        || o.getSourceUpdatedAt().isEqual(observation.getSourceUpdatedAt()))
                .collect(toList());

        Geometry geometry = calculateGeometry(previousObservations);

        return calculateArea(geometry);
    }

    private Double calculateArea(Geometry geometry) {
        double areaInMeters = 0;
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            PolygonArea polygonArea = new PolygonArea(Geodesic.WGS84, false);
            Arrays.stream(geometry.getGeometryN(i).getCoordinates()).forEach(c -> polygonArea.AddPoint(c.getY(), c.getX()));
            areaInMeters += Math.abs(polygonArea.Compute().area);
        }
        double areaInKm = areaInMeters / 1_000_000;
        return areaInKm;
    }

    private Geometry toGeometry(String geometries) {
        return toGeometry(getFirmFeature(geometries));
    }

    private Geometry toGeometry(Feature firmFeature) {
        return geoJSONReader.read(firmFeature.getGeometry(), geometryFactory);
    }

    private Feature getFirmFeature(String geometries) {
        return getFirmFeature(getFeatureCollection(geometries));
    }

    private Feature getFirmFeature(FeatureCollection featureCollection) {
        return getOnlyElement(asList(featureCollection.getFeatures()));
    }

    private FeatureCollection getFeatureCollection(String geometries) {
        return readJson(geometries, FeatureCollection.class);
    }

    private List<NormalizedObservation> readObservations(List<UUID> observationsIds, Set<NormalizedObservation> eventObservations) {
        List<NormalizedObservation> observationsByIds = eventObservations
                .stream()
                .filter(e -> observationsIds.contains(e.getObservationId()))
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