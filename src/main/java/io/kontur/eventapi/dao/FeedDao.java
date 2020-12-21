package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FeedDao {

    private final FeedMapper mapper;

    @Autowired
    public FeedDao(FeedMapper mapper) {
        this.mapper = mapper;
    }

    public List<Feed> getFeeds() {
        return mapper.getFeeds();
    }

    public void insertFeedData(FeedData feedData) {
        String episodesJson = writeJson(feedData.getEpisodes());
        mapper.insertFeedData(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion(),
                feedData.getName(), feedData.getDescription(),
                feedData.getStartedAt(), feedData.getEndedAt(), feedData.getUpdatedAt(),
                feedData.getObservations(), episodesJson);
    }

    public List<FeedData> searchForEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
                                          OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
                                          List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bBox) {
        if(bBox != null){
            var xMin = bBox.get(0);
            var yMin = bBox.get(1);
            var xMax = bBox.get(2);
            var yMax = bBox.get(3);
            return mapper.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder, xMin, xMax, yMin, yMax);
        }
        return mapper.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder, null, null, null, null);
    }

    public Optional<FeedData> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version) {
        return mapper.getEventByEventIdAndByVersionOrLast(eventId, feed, version);
    }

    public Optional<FeedData> getLastFeedData(UUID eventId, UUID feedId) {
        return mapper.getLastFeedData(eventId, feedId);
    }
}
