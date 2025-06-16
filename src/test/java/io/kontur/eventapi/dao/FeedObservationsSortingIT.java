package io.kontur.eventapi.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedObservationsSortingIT extends AbstractCleanableIntegrationTest {

    private final FeedDao feedDao;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FeedObservationsSortingIT(JdbcTemplate jdbcTemplate, FeedDao feedDao) {
        super(jdbcTemplate, feedDao);
        this.feedDao = feedDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void testObservationsAndEpisodesAreSorted() throws Exception {
        Feed feed = feedDao.getFeeds().get(0);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Set<UUID> unsorted = new LinkedHashSet<>();
        unsorted.add(id2);
        unsorted.add(id1);

        FeedEpisode later = new FeedEpisode();
        later.setStartedAt(java.time.OffsetDateTime.parse("2024-01-02T00:00:00Z"));
        later.setEndedAt(java.time.OffsetDateTime.parse("2024-01-03T00:00:00Z"));
        later.setObservations(new LinkedHashSet<>(List.of(id2, id1)));

        FeedEpisode earlier = new FeedEpisode();
        earlier.setStartedAt(java.time.OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        earlier.setEndedAt(java.time.OffsetDateTime.parse("2024-01-01T12:00:00Z"));
        earlier.setObservations(new LinkedHashSet<>(List.of(id2, id1)));

        FeedData feedData = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);
        feedData.setObservations(unsorted);
        // intentionally put later episode first
        feedData.setEpisodes(List.of(later, earlier));
        feedData.setEnriched(true);

        feedDao.insertFeedData(feedData, feed.getAlias());

        UUID[] storedObservations = jdbcTemplate.queryForObject(
                "select observations from feed_data where event_id=?",
                (rs, rowNum) -> (UUID[]) rs.getArray(1).getArray(),
                feedData.getEventId());
        assertArrayEquals(new UUID[]{id1, id2}, storedObservations);

        String episodesJson = jdbcTemplate.queryForObject(
                "select episodes from feed_data where event_id=?",
                String.class, feedData.getEventId());
        JsonNode node = new ObjectMapper().readTree(episodesJson);
        // verify episodes order by startedAt
        assertEquals("2024-01-01T00:00:00Z", node.get(0).get("startedAt").asText());
        assertEquals("2024-01-02T00:00:00Z", node.get(1).get("startedAt").asText());

        List<String> episodeObs = StreamSupport.stream(node.get(0).get("observations").spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
        assertEquals(List.of(id1.toString(), id2.toString()), episodeObs);
    }
}
