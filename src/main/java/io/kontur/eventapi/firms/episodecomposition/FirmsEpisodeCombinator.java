package io.kontur.eventapi.firms.episodecomposition;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.firms.FirmsUtil;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.math.NumberUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.springframework.stereotype.Component;
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
import static io.kontur.eventapi.util.GeometryUtil.calculateAreaKm2;
import static io.kontur.eventapi.util.SeverityUtil.calculateSeverity;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
    @Counted(value = "firmsEpisodeCombinator.processObservation.counter")
    @Timed(value = "firmsEpisodeCombinator.processObservation.timer")
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        Set<FeedEpisode> existingEpisodeForObservation = feedData.getEpisodes()
                .stream()
                .filter(ep -> ep.getSourceUpdatedAt().equals(observation.getSourceUpdatedAt()))
                .collect(toSet());

        FeedEpisode feedEpisode = getOnlyElement(existingEpisodeForObservation, new FeedEpisode());
        populateMissedFields(feedEpisode, observation, feedData, eventObservations);

        return existingEpisodeForObservation.isEmpty() ? List.of(feedEpisode) : emptyList();
    }

    private void populateMissedFields(FeedEpisode episode, NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        Set<NormalizedObservation> episodeObservations = findObservationsForEpisode(
                eventObservations, observation.getSourceUpdatedAt(), 24L, 0L, ChronoUnit.HOURS);
        episode.setObservations(episodeObservations.stream()
                .map(NormalizedObservation::getObservationId)
                .collect(toSet()));

        Geometry episodeGeometry = calculateGeometry(episodeObservations);
        episode.setGeometries(createEpisodeGeometryFeatureCollection(observation, episodeGeometry));

        episode.setDescription(firstNonNull(episode.getDescription(), observation.getEpisodeDescription()));
        episode.setType(firstNonNull(episode.getType(), observation.getType()));
        episode.setStartedAt(firstNonNull(episode.getStartedAt(), observation.getStartedAt()));
        episode.setEndedAt(firstNonNull(episode.getEndedAt(), calculateEndedDate(observation, eventObservations)));
        episode.setUpdatedAt(firstNonNull(episode.getUpdatedAt(), calculateUpdatedDate(episodeObservations)));
        episode.setSourceUpdatedAt(firstNonNull(episode.getSourceUpdatedAt(), observation.getSourceUpdatedAt()));

        Set<NormalizedObservation> observationsUpToCurrentEpisode = eventObservations
                .stream()
                .filter(ob -> ob.getSourceUpdatedAt().isBefore(observation.getSourceUpdatedAt())
                        || ob.getSourceUpdatedAt().isEqual(observation.getSourceUpdatedAt()))
                .collect(toSet());
        long burningTime = observationsUpToCurrentEpisode.stream()
                .map(NormalizedObservation::getStartedAt)
                .min(OffsetDateTime::compareTo)
                .get()
                .until(episode.getEndedAt(), ChronoUnit.HOURS);
        Double area = calculateBurntAreaUpToCurrentEpisode(observation, observationsUpToCurrentEpisode);
        String areaName = getBurntAreaName(episodeObservations);
        episode.setLocation(areaName);
        episode.setSeverity(calculateSeverity(area, burningTime));
        episode.setName(calculateName(areaName, area, burningTime));

    }

    private OffsetDateTime calculateEndedDate(NormalizedObservation observation, Set<NormalizedObservation> eventObservations) {
        return eventObservations
                .stream()
                .map(NormalizedObservation::getStartedAt)
                .filter(startDate -> startDate.isAfter(observation.getStartedAt()))
                .min(OffsetDateTime::compareTo)
                .orElse(observation.getStartedAt().plusHours(24));
    }

    private OffsetDateTime calculateUpdatedDate(Set<NormalizedObservation> episodeObservations) {
        return episodeObservations
                .stream()
                .map(NormalizedObservation::getLoadedAt)
                .max(OffsetDateTime::compareTo)
                .get();
    }

    private Geometry calculateGeometry(Set<NormalizedObservation> observations) {
        Collection<Geometry> geometries = observations
                .stream()
                .map(normalizedObservation -> toGeometry(normalizedObservation.getGeometries()))
                .collect(Collectors.toCollection(HashSet::new));
        return OverlayNGRobust.union(geometries);
    }

    private FeatureCollection createEpisodeGeometryFeatureCollection(NormalizedObservation observation, Geometry geometry) {
        return createFeatureCollection(geometry, getFirmFeature(observation.getGeometries()).getProperties());
    }

    private String calculateName(String areaName, Double area, long burningTime) {
        String burntArea = String.format(Locale.US, "%.3f", area);
        if (!isEmpty(areaName)) {
            areaName = "Thermal anomaly in " + areaName + ". ";
        } else {
            areaName = "Thermal anomaly in an unknown area. ";
        }
        return areaName + "Burnt area " + burntArea + " km\u00B2" + (burningTime > 24 ? ", burning " + burningTime + " hours." : "");
    }

    private String getBurntAreaName(Set<NormalizedObservation> episodeObservations) {
        Geometry centroid = calculateH3Centroid(calculateCentroid(episodeObservations));
        Map<String, Object> params = new HashMap<>();
        params.put("geometry", geoJSONWriter.write(centroid));
        params.put("limit", 10);
        FeatureCollection adminBoundaries = konturApiClient.adminBoundaries(params);
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
                    if (tags.containsKey("name:en")) {
                        return tags.get("name:en");
                    } else if (tags.containsKey("int_name")) {
                        return tags.get("int_name");
                    } else {
                        return tags.get("name");
                    }
                })
                .collect(Collectors.joining(", "));
    }

    private Point calculateCentroid(Set<NormalizedObservation> episodeObservations) {
        List<Geometry> geometries = episodeObservations
                .stream()
                .map(no -> toGeometry(no.getGeometries()))
                .collect(toList());
        return geometryFactory.buildGeometry(geometries).getCentroid();
    }

    private Point calculateH3Centroid(Point geometry) {
        LatLng geoCoord = h3.cellToLatLng(h3.latLngToCell(geometry.getY(), geometry.getX(), 8));
        return geometryFactory.createPoint(new Coordinate(geoCoord.lng, geoCoord.lat));
    }

    private Double calculateBurntAreaUpToCurrentEpisode(NormalizedObservation observation,
                                                        Set<NormalizedObservation> observationsUpToCurrentEpisode) {
        Geometry geometry = calculateGeometry(observationsUpToCurrentEpisode);
        return calculateAreaKm2(geometry);
    }

    private Geometry toGeometry(FeatureCollection geometries) {
        return toGeometry(getFirmFeature(geometries));
    }

    private Geometry toGeometry(Feature firmFeature) {
        return geoJSONReader.read(firmFeature.getGeometry(), geometryFactory);
    }

    private Feature getFirmFeature(FeatureCollection featureCollection) {
        return getOnlyElement(asList(featureCollection.getFeatures()));
    }

    private Set<NormalizedObservation> readObservations(Set<UUID> observationsIds, Set<NormalizedObservation> eventObservations) {
        Set<NormalizedObservation> observationsByIds = eventObservations
                .stream()
                .filter(e -> observationsIds.contains(e.getObservationId()))
                .collect(toSet());

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