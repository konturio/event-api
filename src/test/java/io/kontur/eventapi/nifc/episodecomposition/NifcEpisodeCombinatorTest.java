package io.kontur.eventapi.nifc.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.kontur.eventapi.nifc.converter.NifcDataLakeConverter.NIFC_LOCATIONS_PROVIDER;
import static org.junit.jupiter.api.Assertions.*;

class NifcEpisodeCombinatorTest {

    private final NifcEpisodeCombinator combinator = new NifcEpisodeCombinator();

    @Test
    void isApplicable() {
        NormalizedObservation observation = new NormalizedObservation();
        observation.setProvider(NIFC_LOCATIONS_PROVIDER);
        assertTrue(combinator.isApplicable(observation));
    }

    @Test
    void noDuplicateEpisodeWhenNothingChanged() {
        NormalizedObservation obs1 = createObservation(OffsetDateTime.parse("2024-06-20T00:00:00Z"));
        NormalizedObservation obs2 = createObservation(OffsetDateTime.parse("2024-06-21T00:00:00Z"));

        FeedData feedData = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);
        Set<NormalizedObservation> all = Set.of(obs1, obs2);

        List<FeedEpisode> ep1 = combinator.processObservation(obs1, feedData, all);
        feedData.getEpisodes().addAll(ep1);

        List<FeedEpisode> ep2 = combinator.processObservation(obs2, feedData, all);
        assertTrue(ep2.isEmpty());
    }

    private NormalizedObservation createObservation(OffsetDateTime updatedAt) {
        NormalizedObservation obs = new NormalizedObservation();
        obs.setObservationId(UUID.randomUUID());
        obs.setProvider(NIFC_LOCATIONS_PROVIDER);
        obs.setSourceUpdatedAt(updatedAt);
        obs.setLoadedAt(updatedAt);
        obs.setEndedAt(updatedAt);
        obs.setStartedAt(updatedAt.minusHours(1));
        obs.setName("Wildfire Test");
        obs.setProperName("Test");
        obs.setDescription("desc");
        String geometry = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[10,10]},\"properties\":{}}]}";
        obs.setGeometries((FeatureCollection) GeoJSONFactory.create(geometry));
        return obs;
    }
}
