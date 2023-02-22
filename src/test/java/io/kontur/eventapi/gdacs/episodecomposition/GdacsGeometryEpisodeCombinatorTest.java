package io.kontur.eventapi.gdacs.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GdacsGeometryEpisodeCombinatorTest {

    private final GdacsGeometryEpisodeCombinator episodeCombinator = new GdacsGeometryEpisodeCombinator();

    @Test
    public void testIsApplicable() {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setProvider(GDACS_ALERT_GEOMETRY_PROVIDER);
        assertTrue(episodeCombinator.isApplicable(observation));
    }

    @Test
    public void testProcessObservation() {
        NormalizedObservation alertObservation = createObservation(GDACS_ALERT_PROVIDER);
        NormalizedObservation geometryObservation = createObservation(GDACS_ALERT_GEOMETRY_PROVIDER);
        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);

        List<FeedEpisode> feedEpisodes = episodeCombinator.processObservation(geometryObservation,
                feedData, Set.of(alertObservation, geometryObservation));

        assertTrue(feedEpisodes.isEmpty());
    }

    @Test
    public void testProcessObservationWhenNoAlert() {
        NormalizedObservation geometryObservation = createObservation(GDACS_ALERT_GEOMETRY_PROVIDER);
        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);

        assertThrows(RuntimeException.class, () ->
                episodeCombinator.processObservation(geometryObservation, feedData, Set.of(geometryObservation)));
    }

    private NormalizedObservation createObservation(String provider) {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setObservationId(UUID.randomUUID());
        observation.setProvider(provider);
        observation.setExternalEpisodeId("test_episode_id");
        observation.setSourceUpdatedAt(OffsetDateTime.parse("2021-05-20T00:00:00Z"));
        return observation;
    }

}