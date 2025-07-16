package io.kontur.eventapi.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.wololo.geojson.FeatureCollection;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.kontur.eventapi.TestUtil.readFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicglobalFeedFilterIT extends AbstractCleanableIntegrationTest {

    private final KonturEventsDao konturEventsDao;
    private final FeedEventStatusDao feedEventStatusDao;
    private final FeedDao feedDao;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public MicglobalFeedFilterIT(JdbcTemplate jdbcTemplate, FeedDao feedDao,
                                 KonturEventsDao konturEventsDao, FeedEventStatusDao feedEventStatusDao) {
        super(jdbcTemplate, feedDao);
        this.konturEventsDao = konturEventsDao;
        this.feedEventStatusDao = feedEventStatusDao;
        this.feedDao = feedDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    public void testOutsideUsaEventExcludedFromMicglobal() throws IOException {
        UUID micId = UUID.randomUUID();
        feedDao.createFeed(micId, "micglobal", "Mic Global", List.of("firms.modis-c6"));

        NormalizedObservation obs = new NormalizedObservation();
        obs.setObservationId(UUID.randomUUID());
        obs.setProvider("firms.modis-c6");
        FeatureCollection fc = mapper.readValue(
                readFile(this, "/io/kontur/eventapi/firms/normalization/firms.geometries.json"),
                FeatureCollection.class);
        obs.setGeometries(fc);

        UUID eventId = UUID.randomUUID();
        konturEventsDao.appendObservationIntoEvent(eventId, obs);

        Integer micCount = jdbcTemplate.queryForObject(
                "select count(*) from feed_event_status where feed_id = ?", Integer.class, micId);
        Integer defaultCount = jdbcTemplate.queryForObject(
                "select count(*) from feed_event_status where feed_id = (select feed_id from feeds where alias='test-feed')",
                Integer.class);

        assertEquals(0, micCount);
        assertEquals(1, defaultCount);
    }
}
