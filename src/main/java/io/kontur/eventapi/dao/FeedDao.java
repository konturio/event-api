package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.util.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FeedDao {

    private final FeedMapper mapper;
    private final FeedEventStatusDao feedEventStatusDao;
    private final CacheUtil cacheUtil;

    @Autowired
    public FeedDao(FeedMapper mapper, FeedEventStatusDao feedEventStatusDao, CacheUtil cacheUtil) {
        this.mapper = mapper;
        this.feedEventStatusDao = feedEventStatusDao;
        this.cacheUtil = cacheUtil;
    }

    public List<Feed> getFeeds() {
        return mapper.getFeeds();
    }

    public List<Feed> getFeedsByAliases(List<String> aliases) {
        return mapper.getFeedsByAliases(aliases);
    }

    @Transactional
    public void insertFeedData(FeedData feedData, String feed) {
        String episodesJson = writeJson(feedData.getEpisodes());
        int count = mapper.insertFeedData(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion(),
                feedData.getName(), feedData.getProperName(), feedData.getDescription(), feedData.getType(),
                feedData.getSeverity(), feedData.getActive(), feedData.getStartedAt(), feedData.getEndedAt(),
                feedData.getUpdatedAt(), feedData.getLocation(), feedData.getUrls(), feedData.getLoss(),
                feedData.getObservations(), episodesJson, feedData.getEnriched(), feedData.getAutoExpire(),
                feedData.getGeomFuncType());

        if (count > 0) {
            mapper.markOutdatedEventsVersions(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion());
            feedEventStatusDao.markAsActual(feedData.getFeedId(), feedData.getEventId(), true);
            if (feedData.getEnriched()) {
                cacheUtil.evictEventListCache(feed);
                cacheUtil.evictEventCache(feedData.getEventId(), feed);
            }
        }
    }

    public Optional<Long> getLastFeedDataVersion(UUID eventId, UUID feedId) {
        return mapper.getLastFeedDataVersion(eventId, feedId);
    }

    public List<FeedData> getNotEnrichedEventsForFeed(UUID feedId) {
        return mapper.getNotEnrichedEventsForFeed(feedId);
    }

    public List<FeedData> getEnrichmentSkippedEventsForFeed(UUID feedId) {
        return mapper.getEnrichmentSkippedEventsForFeed(feedId);
    }

    @Transactional
    public void addAnalytics(FeedData event, String feed) {
        mapper.addAnalytics(event.getFeedId(), event.getEventId(), event.getVersion(),
                event.getName(), event.getEventDetails(), writeJson(event.getEpisodes()),
                event.getEnriched(), event.getEnrichmentAttempts(), event.getEnrichmentSkipped());
        if (event.getEnriched()) {
            cacheUtil.evictEventListCache(feed);
            cacheUtil.evictEventCache(event.getEventId(), feed);
        }
    }

    public Integer getNotEnrichedEventsCount() {
        return mapper.getNotEnrichedEventsCount();
    }

    public Integer getEnrichmentSkippedEventsCount() {
        return mapper.getEnrichmentSkippedEventsCount();
    }

    public void createFeed(UUID feedId, String alias, String name, List<String> providers) {
        mapper.createFeed(feedId, alias, name, providers);
    }
}
