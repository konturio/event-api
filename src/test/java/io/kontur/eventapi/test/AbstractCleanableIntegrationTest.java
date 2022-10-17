package io.kontur.eventapi.test;

import io.kontur.eventapi.dao.FeedDao;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.util.Arrays;
import java.util.UUID;

public abstract class AbstractCleanableIntegrationTest extends AbstractIntegrationTest {
    private final JdbcTemplate jdbcTemplate;
    private final FeedDao feedDao;

    @Value("${scheduler.normalization.providers}")
    private String[] providers;

    protected AbstractCleanableIntegrationTest(JdbcTemplate jdbcTemplate, FeedDao feedDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.feedDao = feedDao;
    }

    @BeforeEach
    public void cleanDB() {
        JdbcTestUtils
                .deleteFromTables(jdbcTemplate, "feed_data", "kontur_events", "normalized_observations", "data_lake",
                        "feed_event_status", "feeds");
        feedDao.createFeed(UUID.randomUUID(), "test-feed", "Test Feed", Arrays.asList(providers));
    }
}
