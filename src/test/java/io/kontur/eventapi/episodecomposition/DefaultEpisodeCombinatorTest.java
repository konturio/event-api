package io.kontur.eventapi.episodecomposition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kontur.eventapi.entity.FeedEpisode;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

class DefaultEpisodeCombinatorTest {

    private final DefaultEpisodeCombinator combinator = new DefaultEpisodeCombinator();

    @Test
    void mergesEpisodesWithUnchangedGeometryAndProperties() {
        OffsetDateTime start1 = OffsetDateTime.parse("2023-08-01T00:00:00Z");
        OffsetDateTime end1 = OffsetDateTime.parse("2023-08-01T01:00:00Z");
        OffsetDateTime start2 = end1;
        OffsetDateTime end2 = OffsetDateTime.parse("2023-08-01T02:00:00Z");

        FeatureCollection geom = new FeatureCollection(new Feature[] {
            new Feature(new Point(new double[] {10d, 10d}), null)
        });

        FeedEpisode ep1 = new FeedEpisode();
        ep1.setStartedAt(start1);
        ep1.setEndedAt(end1);
        ep1.setUpdatedAt(start1);
        ep1.setSourceUpdatedAt(start1);
        ep1.setGeometries(geom);
        UUID obs1 = UUID.randomUUID();
        ep1.addObservation(obs1);

        FeedEpisode ep2 = new FeedEpisode();
        ep2.setStartedAt(start2);
        ep2.setEndedAt(end2);
        ep2.setUpdatedAt(start2);
        ep2.setSourceUpdatedAt(start2);
        ep2.setGeometries(geom);
        UUID obs2 = UUID.randomUUID();
        ep2.addObservation(obs2);

        List<FeedEpisode> merged = combinator.postProcessEpisodes(Arrays.asList(ep1, ep2));

        assertEquals(1, merged.size(), "Episodes with same geometry must merge into one");
        FeedEpisode mergedEpisode = merged.get(0);
        assertEquals(start1, mergedEpisode.getStartedAt(), "Merged episode start should be earliest start");
        assertEquals(end2, mergedEpisode.getEndedAt(), "Merged episode end should be latest end");
        assertTrue(mergedEpisode.getObservations().containsAll(Arrays.asList(obs1, obs2)),
            "Merged episode must include all observations");

        OffsetDateTime latestUpdatedAt = ep1.getUpdatedAt().isAfter(ep2.getUpdatedAt())
                ? ep1.getUpdatedAt() : ep2.getUpdatedAt();
        assertEquals(latestUpdatedAt, mergedEpisode.getUpdatedAt(),
                "Merged episode must carry latest updatedAt");

        OffsetDateTime latestSourceUpdatedAt = ep1.getSourceUpdatedAt().isAfter(ep2.getSourceUpdatedAt())
                ? ep1.getSourceUpdatedAt() : ep2.getSourceUpdatedAt();
        assertEquals(latestSourceUpdatedAt, mergedEpisode.getSourceUpdatedAt(),
                "Merged episode must carry latest sourceUpdatedAt");
    }
}
