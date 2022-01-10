package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;
import static io.kontur.eventapi.util.GeometryUtil.calculateAreaKm2;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

@Component
public class ThermalAnomalyTypePostProcessor implements EnrichmentPostProcessor {

    private final static GeoJSONReader geoJSONReader = new GeoJSONReader();
    private final static Map<EventType, String> typeNames = Map.of(
            EventType.INDUSTRIAL_HEAT, "Industrial heat",
            EventType.VOLCANO, "Volcano",
            EventType.WILDFIRE, "Wildfire"
    );

    @Override
    public void process(FeedData event) {
        event.getEpisodes()
                .stream()
                .filter(this::isApplicable)
                .forEach(episode -> {
                    double industrialAreaKm2 = toDouble(episode.getEpisodeDetails().get(INDUSTRIAL_AREA_KM2));
                    double forestAreaKm2 = toDouble(episode.getEpisodeDetails().get(FOREST_AREA_KM2));
                    double volcanoesCount = toDouble(episode.getEpisodeDetails().get(VOLCANOES_COUNT));
                    double hotspotDaysPerYearMax = toDouble(episode.getEpisodeDetails().get(HOTSPOT_DAYS_PER_YEAR_MAX));

                    double area = getArea(episode.getGeometries());

                    if (industrialAreaKm2 > 0) {
                        episode.setType(EventType.INDUSTRIAL_HEAT);
                    } else if (hotspotDaysPerYearMax > 70 && volcanoesCount > 0 && industrialAreaKm2 == 0) {
                        episode.setType(EventType.VOLCANO);
                    } else if (forestAreaKm2 / area > 0.5) {
                        episode.setType(EventType.WILDFIRE);
                    } else {
                        episode.setType(EventType.THERMAL_ANOMALY);
                    }
                    episode.setName(updateName(episode.getName(), episode.getType()));
                });
        event.setName(event.getEpisodes()
                .stream()
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getName)
                .orElse(event.getName()));
    }

    @Override
    public boolean isApplicable(FeedData event) {
        return event.getEpisodes().stream().anyMatch(this::isApplicable);
    }

    public boolean isApplicable(FeedEpisode episode) {
        return episode.getType() == EventType.THERMAL_ANOMALY
                && episode.getEpisodeDetails() != null
                && episode.getEpisodeDetails().containsKey(INDUSTRIAL_AREA_KM2)
                && episode.getEpisodeDetails().containsKey(FOREST_AREA_KM2)
                && episode.getEpisodeDetails().containsKey(VOLCANOES_COUNT)
                && episode.getEpisodeDetails().containsKey(HOTSPOT_DAYS_PER_YEAR_MAX);
    }

    private Double getArea(FeatureCollection fc) {
        Set<Geometry> geometryCollection = Arrays.stream(fc.getFeatures())
                .map(Feature::getGeometry)
                .map(geoJSONReader::read)
                .collect(toSet());
        return calculateAreaKm2(OverlayNGRobust.union(geometryCollection));
    }

    private Double toDouble(Object value) {
        try {
            return value == null ? 0.0 : (double) value;
        } catch (Exception e) {
            return (double) toInteger(value);
        }
    }

    private Integer toInteger(Object value) {
        return value == null ? 0 : (int) value;
    }

    private String updateName(String oldName, EventType type) {
        return typeNames.containsKey(type) ? oldName.replace("Thermal anomaly", typeNames.get(type)) : oldName;
    }
}
