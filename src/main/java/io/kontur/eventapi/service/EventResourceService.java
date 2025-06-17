package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.FeedDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.kontur.eventapi.util.CacheUtil.EVENT_CACHE_NAME;

@Service
public class EventResourceService {

    private final ApiDao apiDao;
    private final Environment environment;
    @Value("${app.enabledFeeds:}")
    private String[] enabledFeeds;
    private static final Logger LOG = LoggerFactory.getLogger(EventResourceService.class);

    public EventResourceService(ApiDao apiDao, Environment environment) {
        this.apiDao = apiDao;
        this.environment = environment;
    }

    public List<FeedDto> getFeeds() {
        List<FeedDto> feeds = apiDao.getFeeds();
        if (enabledFeeds == null || enabledFeeds.length == 0) {
            LOG.debug("All feeds enabled; returning full list");
            return feeds;
        }
        Set<String> allowed = Arrays.stream(enabledFeeds)
                .filter(it -> it != null && !it.isBlank())
                .collect(Collectors.toSet());
        LOG.debug("Enabled feeds from config: {}", allowed);
        return feeds.stream()
                .filter(feed -> allowed.contains(feed.getFeed()))
                .collect(Collectors.toList());
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
        String data = apiDao.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter,
                limit, severities, sortOrder, bbox, episodeFilterType);
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
        String geoJson = apiDao.searchForEventsGeoJson(feedAlias, eventTypes, from, to,
                updatedAfter, limit, severities, sortOrder, bbox, episodeFilterType);
        return geoJson == null ? Optional.empty() : Optional.of(geoJson);
    }
}
