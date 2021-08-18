package io.kontur.eventapi.gdacs.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class GdacsAlertEpisodeCombinatorTest {

    private final GdacsAlertEpisodeCombinator episodeCombinator = new GdacsAlertEpisodeCombinator();

    @Test
    public void testIsApplicable() {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setProvider(GDACS_ALERT_PROVIDER);
        assertTrue(episodeCombinator.isApplicable(observation));
    }

    @Test
    public void testProcessObservation() throws IOException {
        NormalizedObservation alertObservation = createObservation(GDACS_ALERT_PROVIDER, null);
        FeatureCollection geometries = (FeatureCollection) GeoJSONFactory.create(readFile("geometry.json"));
        NormalizedObservation geometryObservation = createObservation(GDACS_ALERT_GEOMETRY_PROVIDER, geometries);
        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);

        Optional<FeedEpisode> feedEpisodeOpt = episodeCombinator.processObservation(alertObservation,
                feedData, Set.of(alertObservation, geometryObservation));

        assertTrue(feedEpisodeOpt.isPresent());
        FeedEpisode feedEpisode = feedEpisodeOpt.get();
        assertEquals(2, feedEpisode.getObservations().size());
        assertTrue(feedEpisode.getObservations().contains(alertObservation.getObservationId()));
        assertTrue(feedEpisode.getObservations().contains(geometryObservation.getObservationId()));
        assertNotNull(feedEpisode.getGeometries());
    }

    @Test
    public void testProcessObservationWhenNoGeometry() throws IOException {
        NormalizedObservation alertObservation = createObservation(GDACS_ALERT_PROVIDER, null);
        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);

        assertThrows(RuntimeException.class, () ->
                episodeCombinator.processObservation(alertObservation, feedData, Set.of(alertObservation)));
    }

    private NormalizedObservation createObservation(String provider, FeatureCollection geometries) {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setObservationId(UUID.randomUUID());
        observation.setProvider(provider);
        observation.setExternalEpisodeId("test_episode_id");
        observation.setSourceUpdatedAt(OffsetDateTime.parse("2021-05-20T00:00:00Z"));
        observation.setGeometries(geometries);
        return observation;
    }

    private String readFile(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName), "UTF-8");
    }

}