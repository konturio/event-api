package io.kontur.eventapi.calfire.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class CalFireEpisodeCombinatorTest {

    private final CalFireEpisodeCombinator combinator = new CalFireEpisodeCombinator();

    @Test
    void testProcessObservationSkipsDuplicate() {
        NormalizedObservation obs1 = createObservation("Fire1");
        NormalizedObservation obs2 = createObservation("Fire1");
        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);

        List<FeedEpisode> first = combinator.processObservation(obs1, feedData, Set.of(obs1));
        feedData.addEpisode(first.get(0));

        List<FeedEpisode> second = combinator.processObservation(obs2, feedData, Set.of(obs1, obs2));

        assertEquals(1, first.size());
        assertTrue(second.isEmpty());
    }

    @Test
    void testProcessObservationCreatesNewEpisodeWhenDifferent() {
        NormalizedObservation obs1 = createObservation("Fire1");
        NormalizedObservation obs2 = createObservation("Fire2");
        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);

        feedData.addEpisode(combinator.processObservation(obs1, feedData, Set.of(obs1)).get(0));

        List<FeedEpisode> episodes = combinator.processObservation(obs2, feedData, Set.of(obs1, obs2));

        assertFalse(episodes.isEmpty());
    }

    private NormalizedObservation createObservation(String name) {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setObservationId(UUID.randomUUID());
        observation.setProvider(CALFIRE_PROVIDER);
        observation.setName(name);
        observation.setEventSeverity(Severity.MINOR);
        observation.setSourceUpdatedAt(OffsetDateTime.now());
        observation.setLoadedAt(OffsetDateTime.now());
        Feature feature = new Feature(new Point(new double[]{0, 0}), java.util.Collections.emptyMap());
        observation.setGeometries(new FeatureCollection(new Feature[]{feature}));
        return observation;
    }
}
