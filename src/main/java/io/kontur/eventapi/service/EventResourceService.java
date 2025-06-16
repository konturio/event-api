package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.FeedDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.util.CacheUtil.EVENT_CACHE_NAME;
import static io.kontur.eventapi.util.CacheUtil.FEED_CACHE_NAME;

@Service
public class EventResourceService {

    private final ApiDao apiDao;
    private final Environment environment;

    public EventResourceService(ApiDao apiDao, Environment environment) {
        this.apiDao = apiDao;
        this.environment = environment;
    }

    @Cacheable(cacheNames = FEED_CACHE_NAME, condition = "#root.target.isCacheEnabled()")
    public List<FeedDto> getFeeds() {
        return apiDao.getFeeds();
    }

    public Optional<String> getRawData(UUID observationId) {
        return apiDao.findDataByObservationId(observationId);
    }

    public boolean isCacheEnabled() {
        return !Arrays.asList(environment.getActiveProfiles()).contains("cacheDisabled");
    }

    @Cacheable(cacheResolver = "cacheResolver", condition = "#root.target.isCacheEnabled()")
    public Optional<String> searchEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
                                       OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
                                       List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bbox,
                                       EpisodeFilterType episodeFilterType, OffsetDateTime episodeUpdatedAfter) {
        String data = apiDao.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter,
                limit, severities, sortOrder, bbox, episodeFilterType, episodeUpdatedAfter);
        return data == null ? Optional.empty() : Optional.of(data);
    }

    @Cacheable(cacheNames = EVENT_CACHE_NAME, cacheManager = "longCacheManager", condition = "#root.target.isCacheEnabled()")
    public Optional<String> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version, EpisodeFilterType episodeFilterType, OffsetDateTime episodeUpdatedAfter) {
        return apiDao.getEventByEventIdAndByVersionOrLast(eventId, feed, version, episodeFilterType, episodeUpdatedAfter);
    }

    public Optional<String> searchEventsGeoJson(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
                                                OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
                                                List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bbox,
                                                EpisodeFilterType episodeFilterType, OffsetDateTime episodeUpdatedAfter) {
        String geoJson = apiDao.searchForEventsGeoJson(feedAlias, eventTypes, from, to,
                updatedAfter, limit, severities, sortOrder, bbox, episodeFilterType, episodeUpdatedAfter);
        return geoJson == null ? Optional.empty() : Optional.of(geoJson);
    }
}
