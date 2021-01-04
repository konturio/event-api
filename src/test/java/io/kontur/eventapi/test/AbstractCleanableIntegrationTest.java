package io.kontur.eventapi.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

public abstract class AbstractCleanableIntegrationTest extends AbstractIntegrationTest {
    private final JdbcTemplate jdbcTemplate;

    protected AbstractCleanableIntegrationTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    public void cleanDB() {
        JdbcTestUtils
                .deleteFromTables(jdbcTemplate, "feed_data", "kontur_events", "normalized_observations", "data_lake",
                        "feed_event_status");
    }
}
