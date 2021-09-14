package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import org.springframework.stereotype.Component;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.geojson.GeometryCollection;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.Arrays;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;

@Component
public class ThermalAnomalyTypePostProcessor implements EnrichmentPostProcessor {

    private final static GeoJSONReader geoJSONReader = new GeoJSONReader();

    @Override
    public void process(FeedData event) {
        event.getEpisodes()
                .stream()
                .filter(this::isApplicable)
                .forEach(episode -> {
                    double industrialAreaKm2 = toDouble(episode.getEpisodeDetails().get(INDUSTRIAL_AREA_KM2));
                    double forestAreaKm2 = toDouble(episode.getEpisodeDetails().get(FOREST_AREA_KM2));
                    long volcanoesCount = toLong(episode.getEpisodeDetails().get(VOLCANOES_COUNT));
                    long hotspotDaysPerYearMax = toLong(episode.getEpisodeDetails().get(HOTSPOT_DAYS_PER_YEAR_MAX));

                    double area = geoJSONReader.read(toGeometryCollection(episode.getGeometries())).getArea();

                    if (industrialAreaKm2 > 0) {
                        episode.setType(EventType.INDUSTRIAL_HEAT);
                    } else if (hotspotDaysPerYearMax > 70 && volcanoesCount > 0 && industrialAreaKm2 == 0) {
                        episode.setType(EventType.VOLCANO);
                    } else if (forestAreaKm2 / area > 0.5) {
                        episode.setType(EventType.WILDFIRE);
                    } else {
                        episode.setType(EventType.THERMAL_ANOMALY);
                    }
                });
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

    private GeometryCollection toGeometryCollection(FeatureCollection featureCollection) {
        return new GeometryCollection(Arrays.stream(featureCollection.getFeatures())
                .map(Feature::getGeometry)
                .toArray(Geometry[]::new));
    }

    private Double toDouble(Object value) {
        return value == null ? 0 : (Double) value;
    }

    private Long toLong(Object value) {
        return value == null ? 0 : (Long) value;
    }
}
