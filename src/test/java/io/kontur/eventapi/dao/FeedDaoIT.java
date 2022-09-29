package io.kontur.eventapi.dao;

import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedDaoIT extends AbstractCleanableIntegrationTest {

    private final FeedDao feedFao;

    @Autowired
    public FeedDaoIT(JdbcTemplate jdbcTemplate, FeedDao feedFao) {
        super(jdbcTemplate);
        this.feedFao = feedFao;
    }

    @Test
    public void testGetFeedByAlias() {
        List<Feed> feedsByAliases = feedFao.getFeedsByAliases(Collections.singletonList("kontur-public"));

        assertEquals(1, feedsByAliases.size());
        assertEquals("kontur-public", feedsByAliases.get(0).getAlias());
        assertEquals("Kontur Public", feedsByAliases.get(0).getName());
    }
}