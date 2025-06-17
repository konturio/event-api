package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.FeedDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class EventResourceService {

    private static final Logger logger = LoggerFactory.getLogger(EventResourceService.class);

    private final ApiDao apiDao;
    private final Environment environment;

    public EventResourceService(ApiDao apiDao, Environment environment) {
        this.apiDao = apiDao;
        this.environment = environment;
    }

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
                                       EpisodeFilterType episodeFilterType) {
        long start = System.currentTimeMillis();
        String data = apiDao.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter,
                limit, severities, sortOrder, bbox, episodeFilterType);
        long duration = System.currentTimeMillis() - start;
        logger.debug("searchEvents feed={} eventTypes={} bboxPresent={} duration={}ms",
                feedAlias, eventTypes, bbox != null, duration);
        return data == null ? Optional.empty() : Optional.of(data);
    }

    @Cacheable(cacheNames = EVENT_CACHE_NAME, cacheManager = "longCacheManager", condition = "#root.target.isCacheEnabled()")
    public Optional<String> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version, EpisodeFilterType episodeFilterType) {
        return apiDao.getEventByEventIdAndByVersionOrLast(eventId, feed, version, episodeFilterType);
    }

    public Optional<String> searchEventsGeoJson(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
                                                OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
                                                List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bbox,
                                                EpisodeFilterType episodeFilterType) {
        long start = System.currentTimeMillis();
        String geoJson = apiDao.searchForEventsGeoJson(feedAlias, eventTypes, from, to,
                updatedAfter, limit, severities, sortOrder, bbox, episodeFilterType);
        long duration = System.currentTimeMillis() - start;
        logger.debug("searchEventsGeoJson feed={} eventTypes={} bboxPresent={} duration={}ms",
                feedAlias, eventTypes, bbox != null, duration);
        return geoJson == null ? Optional.empty() : Optional.of(geoJson);
    }
}
