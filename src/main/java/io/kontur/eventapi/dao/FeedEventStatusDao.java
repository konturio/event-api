package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.FeedEventStatusMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FeedEventStatusDao {

    private final FeedEventStatusMapper mapper;

    @Autowired
    public FeedEventStatusDao(FeedEventStatusMapper mapper) {
        this.mapper = mapper;
    }

    public void markAsActual(UUID feedId, UUID eventId, boolean actual) {
        mapper.markAsActual(feedId, eventId, actual);
    }

    public void markAsNonActual(String provider, UUID eventId) {
        mapper.markAsNonActual(provider, eventId);
    }

    public void markAsNonActualExcludeFeed(String provider, UUID eventId, String excludeAlias) {
        mapper.markAsNonActualExcludeFeed(provider, eventId, excludeAlias);
    }
}
