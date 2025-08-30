package io.kontur.eventapi.job;

import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.util.GeometryUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedCompositionJobTest {

    @Test
    void buildEpisodesDebugInfoEmptyList() {
        String info = FeedCompositionJob.buildEpisodesDebugInfo(Collections.emptyList());
        assertEquals("[]", info, "Empty episodes debug string mismatch: " + info);
    }

    @Test
    void buildEpisodesDebugInfoHandlesNullGeometries() {
        FeedEpisode episode = new FeedEpisode();
        episode.setGeometries(null);
        String info = FeedCompositionJob.buildEpisodesDebugInfo(Collections.singletonList(episode));
        assertTrue(info.contains("geometry={}"),
                "Empty geometry token missing in debug info: " + info);
    }

    @Test
    void buildEpisodesDebugInfoHandlesMultiLineStringLength() throws Exception {
        FeedEpisode episode = new FeedEpisode();
        String fcString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiLineString\",\"coordinates\":[[[0,0],[1,0]],[[1,0],[1,1]]]},\"properties\":{}}]}";
        FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(fcString);
        episode.setGeometries(fc);

        String info = FeedCompositionJob.buildEpisodesDebugInfo(Collections.singletonList(episode));

        double length = GeometryUtil.calculateLengthKm(
                FeedCompositionJob.getGeoJsonReader().read(fc.getFeatures()[0].getGeometry()));
        assertTrue(info.contains("lengthKm=" + String.format(Locale.ROOT, "%.2f", length)),
                "Geometry length missing in debug info: " + info);
    }

    @Test
    void buildEpisodesDebugInfoIncludesGeometryData() throws Exception {
        FeedEpisode episode = new FeedEpisode();
        episode.setStartedAt(OffsetDateTime.parse("2020-01-01T00:00Z"));
        episode.setEndedAt(OffsetDateTime.parse("2020-01-02T00:00Z"));
        UUID obsId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        episode.addObservation(obsId);
        String fcString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[0,1],[1,1],[1,0],[0,0]]]},\"properties\":{}}]}";
        FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(fcString);
        episode.setGeometries(fc);

        String info = FeedCompositionJob.buildEpisodesDebugInfo(Collections.singletonList(episode));

        byte[] bytes = FeedCompositionJob.getMapper().writeValueAsBytes(fc);
        String hash = DigestUtils.md5Hex(bytes);
        int length = bytes.length;
        double area = GeometryUtil.calculateAreaKm2(
                FeedCompositionJob.getGeoJsonReader().read(fc.getFeatures()[0].getGeometry()));

        assertTrue(info.contains("start=2020-01-01T00:00Z"),
                "Start time missing in debug info: " + info);
        assertTrue(info.contains("end=2020-01-02T00:00Z"),
                "End time missing in debug info: " + info);
        assertTrue(info.contains(obsId.toString()),
                "Observation ID missing in debug info: " + info);
        assertTrue(info.contains(hash),
                "Geometry hash missing in debug info: " + info);
        assertTrue(info.contains("bytes=" + length),
                "Geometry byte length missing in debug info: " + info);
        assertTrue(info.contains("areaKm2=" + String.format(Locale.ROOT, "%.2f", area)),
                "Geometry area missing in debug info: " + info);
    }

    @Test
    void buildEpisodesDebugInfoIncludesGeometryLength() throws Exception {
        FeedEpisode episode = new FeedEpisode();
        episode.setStartedAt(OffsetDateTime.parse("2020-01-01T00:00Z"));
        episode.setEndedAt(OffsetDateTime.parse("2020-01-02T00:00Z"));
        UUID obsId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        episode.addObservation(obsId);
        String fcString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[0,0],[1,0]]},\"properties\":{}}]}";
        FeatureCollection fc = (FeatureCollection) GeoJSONFactory.create(fcString);
        episode.setGeometries(fc);

        String info = FeedCompositionJob.buildEpisodesDebugInfo(Collections.singletonList(episode));

        double length = GeometryUtil.calculateLengthKm(
                FeedCompositionJob.getGeoJsonReader().read(fc.getFeatures()[0].getGeometry()));

        assertTrue(info.contains(obsId.toString()),
                "Observation ID missing in debug info: " + info);
        assertTrue(info.contains("lengthKm=" + String.format(Locale.ROOT, "%.2f", length)),
                "Geometry length missing in debug info: " + info);
    }
}
