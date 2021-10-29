package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FeedDao {

    private final FeedMapper mapper;
    private final FeedEventStatusDao feedEventStatusDao;

    @Autowired
    public FeedDao(FeedMapper mapper, FeedEventStatusDao feedEventStatusDao) {
        this.mapper = mapper;
        this.feedEventStatusDao = feedEventStatusDao;
    }

    public List<Feed> getFeeds() {
        return mapper.getFeeds();
    }

    public List<Feed> getFeedsByAliases(List<String> aliases) {
        return mapper.getFeedsByAliases(aliases);
    }

    @Transactional
    public void insertFeedData(FeedData feedData) {
        String episodesJson = writeJson(feedData.getEpisodes());
        mapper.insertFeedData(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion(),
                feedData.getName(), feedData.getProperName(), feedData.getDescription(),
                feedData.getStartedAt(), feedData.getEndedAt(), feedData.getUpdatedAt(),
                feedData.getObservations(), episodesJson, feedData.getEnriched(), feedData.getUrls());

        mapper.markOutdatedEventsVersions(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion());
        feedEventStatusDao.markAsActual(feedData.getFeedId(), feedData.getEventId(), true);
    }

    public List<FeedData> searchForEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
                                          OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
                                          List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bBox,
                                          EpisodeFilterType episodeFilterType) {
        if(bBox != null){
            var xMin = bBox.get(0);
            var yMin = bBox.get(1);
            var xMax = bBox.get(2);
            var yMax = bBox.get(3);
            return mapper.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder,
                    xMin, xMax, yMin, yMax, episodeFilterType);
        }
        return mapper.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder,
                null, null, null, null, episodeFilterType);
    }

    public Optional<FeedData> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version) {
        return mapper.getEventByEventIdAndByVersionOrLast(eventId, feed, version);
    }

    public Optional<Long> getLastFeedDataVersion(UUID eventId, UUID feedId) {
        return mapper.getLastFeedDataVersion(eventId, feedId);
    }

    public List<FeedData> getNotEnrichedEventsForFeed(UUID feedId) {
        return mapper.getNotEnrichedEventsForFeed(feedId);
    }

    @Transactional
    public void addAnalytics(FeedData event) {
        mapper.addAnalytics(event.getFeedId(), event.getEventId(), event.getVersion(),
                writeJson(event.getEventDetails()), event.getEnriched(), writeJson(event.getEpisodes()),
                event.getName(), event.getEnrichmentAttempts(), event.getEnrichmentSkipped());
    }

    public Integer getNotEnrichedEventsCount() {
        return mapper.getNotEnrichedEventsCount();
    }
}
