package io.kontur.eventapi.enrichment.postprocessor;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.enrichment.EnrichmentConfig;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThermalAnomalyTypePostProcessorTest {

    private final ThermalAnomalyTypePostProcessor postProcessor = new ThermalAnomalyTypePostProcessor();
    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

    @Test
    public void shouldClassifyEpisodes() throws Exception {
        Feed feed = new Feed();
        feed.setFeedId(UUID.randomUUID());
        feed.setAlias("test-feed");
        feed.setEnrichmentPostProcessors(List.of(EnrichmentConfig.WILDFIRE_TYPE_POSTPROCESSOR));

        FeedData feedData = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);

        // INDUSTRIAL_HEAT
        feedData.addEpisode(createEpisode(10, 0, 0, 0, "POINT(0 0)"));
        // VOLCANO
        feedData.addEpisode(createEpisode(0, 0, 2, 80, "POINT(0 0)"));
        // WILDFIRE
        double area = calculateArea("POLYGON((0 0,0 0.1,0.1 0.1,0.1 0,0 0))");
        feedData.addEpisode(createEpisode(0, area * 0.6, 0, 0, "POLYGON((0 0,0 0.1,0.1 0.1,0.1 0,0 0))"));
        // THERMAL_ANOMALY
        feedData.addEpisode(createEpisode(0, 0, 0, 0, "POINT(0 0)"));

        postProcessor.process(feedData);

        List<EventType> types = feedData.getEpisodes().stream().map(FeedEpisode::getType).toList();
        assertEquals(List.of(EventType.INDUSTRIAL_HEAT, EventType.VOLCANO,
                EventType.WILDFIRE, EventType.THERMAL_ANOMALY), types);
    }

    private FeedEpisode createEpisode(double industrialArea, double forestArea, double volcanoes, double hotspot, String wkt)
            throws ParseException {
        FeedEpisode episode = new FeedEpisode();
        episode.setType(EventType.THERMAL_ANOMALY);

        Map<String, Object> details = new HashMap<>();
        details.put(EnrichmentConfig.INDUSTRIAL_AREA_KM2, industrialArea);
        details.put(EnrichmentConfig.FOREST_AREA_KM2, forestArea);
        details.put(EnrichmentConfig.VOLCANOES_COUNT, volcanoes);
        details.put(EnrichmentConfig.HOTSPOT_DAYS_PER_YEAR_MAX, hotspot);
        episode.setEpisodeDetails(details);

        Geometry geometry = wktReader.read(wkt);
        episode.setGeometries(new FeatureCollection(new Feature[]{
                new Feature(geoJSONWriter.write(geometry), Map.of())
        }));
        return episode;
    }

    private double calculateArea(String wkt) throws ParseException {
        Geometry geometry = wktReader.read(wkt);
        Geometry union = OverlayNGRobust.union(Set.of(geometry));
        return io.kontur.eventapi.util.GeometryUtil.calculateAreaKm2(union);
    }
}
