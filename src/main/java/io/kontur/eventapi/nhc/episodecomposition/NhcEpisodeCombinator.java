package io.kontur.eventapi.nhc.episodecomposition;

import static io.kontur.eventapi.nhc.NhcUtil.COEFFICIENT_KNOTS_TO_KPH;
import static io.kontur.eventapi.nhc.NhcUtil.NHC_PROVIDERS;
import static io.kontur.eventapi.nhc.NhcUtil.SEVERITY_MINOR_MAX_WIND_SPEED;
import static io.kontur.eventapi.nhc.NhcUtil.SEVERITY_MODERATE_MAX_WIND_SPEED;
import static io.kontur.eventapi.nhc.NhcUtil.SEVERITY_SEVERE_MAX_WIND_SPEED;
import static io.kontur.eventapi.util.GeometryUtil.ALERT_AREA;
import static io.kontur.eventapi.util.GeometryUtil.AREA_TYPE_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.FORECAST_HRS_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.IS_OBSERVED_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.POSITION;
import static io.kontur.eventapi.util.GeometryUtil.TIMESTAMP_PROPERTY;
import static io.kontur.eventapi.util.GeometryUtil.WIND_SPEED_KPH;
import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.util.DateTimeUtil;
import liquibase.repackaged.org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;
import org.wololo.jts2geojson.GeoJSONWriter;

@Component
public class NhcEpisodeCombinator extends EpisodeCombinator {

    private static final Logger LOG = LoggerFactory.getLogger(NhcEpisodeCombinator.class);

    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return NHC_PROVIDERS.contains(observation.getProvider());
    }

    @Override
    public Optional<List<FeedEpisode>> processObservation(NormalizedObservation observation, FeedData feedData,
                                                    Set<NormalizedObservation> eventObservations) {
        if (episodeExistsForObservation(feedData.getEpisodes(), observation)) {
            return Optional.empty();
        }
        feedData.setGeomFuncType(NHC_GEOMETRY_FUNCTION);
        NormalizedObservation latestObservation = findLatestEpisodeObservation(eventObservations);
        if (latestObservation != null && latestObservation.getObservationId() != null) {
            if (!latestObservation.getObservationId().equals(observation.getObservationId())) {
                // previous observations - get point with isObserved=true as past episode
                if (observation.getGeometries() != null
                        && ArrayUtils.isNotEmpty(observation.getGeometries().getFeatures())) {
                    Feature pointFeature = Arrays.stream(observation.getGeometries().getFeatures())
                            .filter(f -> (!f.getProperties().isEmpty()
                                    && Boolean.TRUE.equals(f.getProperties().get(IS_OBSERVED_PROPERTY))))
                            .findFirst()
                            .orElse(null);
                    FeedEpisode episode = createFeedEpisodeAndFill(observation, pointFeature);
                    return episode != null ? Optional.of(List.of(episode)) : Optional.empty();
                }
            } else {
                // latest - get point with isObserved=true as current episode and other points as forecast episodes
                if (observation.getGeometries() != null
                        && ArrayUtils.isNotEmpty(observation.getGeometries().getFeatures())) {
                    List<FeedEpisode> episodes = new ArrayList<>();
                    Feature currentPoint = Arrays.stream(observation.getGeometries().getFeatures())
                            .filter(f -> (!f.getProperties().isEmpty()
                                    && Boolean.TRUE.equals(f.getProperties().get(IS_OBSERVED_PROPERTY))))
                            .findFirst()
                            .orElse(null);
                    FeedEpisode currentEpisode = createFeedEpisodeAndFill(observation, currentPoint);
                    if (currentEpisode != null) {
                        episodes.add(currentEpisode);
                    }

                    List<Feature> forecastPoints = Arrays.stream(observation.getGeometries().getFeatures())
                            .filter(f -> (!f.getProperties().isEmpty()
                                    && Boolean.FALSE.equals(f.getProperties().get(IS_OBSERVED_PROPERTY))))
                            .toList();
                    if (!CollectionUtils.isEmpty(forecastPoints)) {
                        forecastPoints.forEach(forecastPoint -> {
                            FeedEpisode forecastEpisode = createFeedEpisodeAndFill(observation, forecastPoint);
                            if (forecastEpisode != null) {
                                episodes.add(forecastEpisode);
                            }
                        });
                    }
                    return !CollectionUtils.isEmpty(episodes) ? Optional.of(episodes) : Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private FeedEpisode createFeedEpisodeAndFill(NormalizedObservation obs, Feature pointFeature) {
        FeedEpisode episode = new FeedEpisode();
        episode.setName(obs.getName());
        episode.setType(obs.getType());
        episode.setActive(obs.getActive());
        episode.setUpdatedAt(obs.getLoadedAt());
        episode.setSourceUpdatedAt(obs.getSourceUpdatedAt());
        episode.setDescription(obs.getEpisodeDescription());
        episode.addObservation(obs.getObservationId());
        episode.setProperName(obs.getProperName());
        episode.setLocation(obs.getRegion());
        if (!CollectionUtils.isEmpty(obs.getUrls())) {
            episode.addUrlIfNotExists(obs.getUrls());
        }

        if (pointFeature != null && MapUtils.isNotEmpty(pointFeature.getProperties())) {
            Map<String, Object> props = pointFeature.getProperties();
            double windSpeed;
            try {
                windSpeed = Double.parseDouble(String.valueOf(props.get(WIND_SPEED_KPH))) / 1.852;
                if (windSpeed < SEVERITY_MINOR_MAX_WIND_SPEED) {
                    episode.setSeverity(Severity.MINOR);
                } else if (windSpeed <= SEVERITY_MODERATE_MAX_WIND_SPEED) {
                    episode.setSeverity(Severity.MODERATE);
                } else if (windSpeed <= SEVERITY_SEVERE_MAX_WIND_SPEED) {
                    episode.setSeverity(Severity.SEVERE);
                } else {
                    episode.setSeverity(Severity.EXTREME);
                }
            } catch (Exception e) {
                LOG.warn("Error while converting wind speed in composition for {}", obs.getObservationId());
                return null;
            }
            try {
                episode.setStartedAt(DateTimeUtil.parseDateTimeByPattern((String) props.get(TIMESTAMP_PROPERTY), null));
            } catch (Exception e) {
                episode.setStartedAt(obs.getStartedAt());
            }
            episode.setEndedAt(episode.getStartedAt());

            // Create alert areas from point
            List<Feature> featureList = new ArrayList<>();
            pointFeature.getProperties().put(AREA_TYPE_PROPERTY, POSITION);
            featureList.add(pointFeature);
            createEpisodeGeometryFeature(pointFeature, props, 64, obs.getObservationId())
                    .ifPresent(featureList::add);
            createEpisodeGeometryFeature(pointFeature, props, 50, obs.getObservationId())
                    .ifPresent(featureList::add);
            createEpisodeGeometryFeature(pointFeature, props, 34, obs.getObservationId())
                    .ifPresent(featureList::add);

            if (!CollectionUtils.isEmpty(featureList)) {
                episode.setGeometries(new FeatureCollection(featureList.toArray(new Feature[0])));
            }
        }
        return episode;
    }

    private Optional<Feature> createEpisodeGeometryFeature(Feature sourceFeature, Map<String, Object> props,
                                                           Integer level, UUID observationId) {
        List<Double> bufferLen = new ArrayList<>();
        if (props.get(level + "_kt_NE") != null) {
            bufferLen.add(Double.parseDouble(String.valueOf(props.get(level + "_kt_NE"))));
        }
        if (props.get(level + "_kt_SE") != null) {
            bufferLen.add(Double.parseDouble(String.valueOf(props.get(level + "_kt_SE"))));
        }
        if (props.get(level + "_kt_SW") != null) {
            bufferLen.add(Double.parseDouble(String.valueOf(props.get(level + "_kt_SW"))));
        }
        if (props.get(level + "_kt_NW") != null) {
            bufferLen.add(Double.parseDouble(String.valueOf(props.get(level + "_kt_NW"))));
        }
        if (!CollectionUtils.isEmpty(bufferLen)) {
            double length = bufferLen.stream().max(Double::compareTo).orElse(Double.MIN_VALUE);
            Point point = (Point) sourceFeature.getGeometry();
            GeometryFactory factory = new GeometryFactory();
            org.locationtech.jts.geom.Point destPoint =
                    factory.createPoint(new Coordinate(point.getCoordinates()[0], point.getCoordinates()[1]));
            org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) destPoint.buffer(length);
            try {
                Map<String, Object> properties = new HashMap<>();
                properties.put(AREA_TYPE_PROPERTY, ALERT_AREA);
                properties.put(IS_OBSERVED_PROPERTY, props.get(IS_OBSERVED_PROPERTY));
                if (props.get(FORECAST_HRS_PROPERTY) != null) {
                    properties.put(FORECAST_HRS_PROPERTY, props.get(FORECAST_HRS_PROPERTY));
                }
                properties.put(TIMESTAMP_PROPERTY, props.get(TIMESTAMP_PROPERTY));
                properties.put(WIND_SPEED_KPH, Math.round(level * COEFFICIENT_KNOTS_TO_KPH));

                return Optional.of(new Feature(geoJSONWriter.write(wktReader.read(polygon.toText())), properties));
            } catch (Exception e) {
                LOG.warn("Error while convert point to polygon with wind speed {} for observation {}", level,
                        observationId);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        if (CollectionUtils.isEmpty(episodes)) {
            return episodes;
        }
        if (episodes.size() < 2) {
            episodes.get(0).setEndedAt(episodes.get(0).getStartedAt());
            return episodes;
        }

        episodes.sort(comparing(FeedEpisode::getStartedAt));
        Iterator<FeedEpisode> it = episodes.iterator();
        FeedEpisode prev = null;
        while (it.hasNext()) {
            FeedEpisode next = it.next();
            if (prev != null) {
                prev.setEndedAt(next.getStartedAt());
            }
            prev = next;
        }
        if (prev != null) {
            prev.setEndedAt(prev.getStartedAt());
        }
        return episodes;
    }
}
