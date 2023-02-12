package io.kontur.eventapi.service;

import io.kontur.eventapi.converter.EventDtoConverter;
import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.EventDto;
import io.kontur.eventapi.resource.dto.FeedDto;
import io.kontur.eventapi.resource.dto.GeoJsonPaginationDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.util.CacheUtil.EVENT_CACHE_NAME;

@Service
public class EventResourceService {

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
    public List<EventDto> searchEvents(String feedAlias,
                                       List<EventType> eventTypes,
                                       OffsetDateTime from,
                                       OffsetDateTime to,
                                       OffsetDateTime updatedAfter,
                                       int limit,
                                       List<Severity> severities,
                                       SortOrder sortOrder,
                                       List<BigDecimal> bbox,
                                       EpisodeFilterType episodeFilterType) {
        return apiDao.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter,
                limit, severities, sortOrder, bbox, episodeFilterType);
    }

    @Cacheable(cacheNames = EVENT_CACHE_NAME, cacheManager = "longCacheManager", condition = "#root.target.isCacheEnabled()")
    public Optional<EventDto> getEventByEventIdAndByVersionOrLast(UUID eventId,
                                                                  String feed,
                                                                  Long version,
                                                                  EpisodeFilterType episodeFilterType) {
        return apiDao.getEventByEventIdAndByVersionOrLast(eventId, feed, version, episodeFilterType);
    }

    public Optional<GeoJsonPaginationDTO> searchEventsGeoJson(String feedAlias,
                                                              List<EventType> eventTypes,
                                                              OffsetDateTime from,
                                                              OffsetDateTime to,
                                                              OffsetDateTime updatedAfter,
                                                              int limit,
                                                              List<Severity> severities,
                                                              SortOrder sortOrder,
                                                              List<BigDecimal> bbox,
                                                              EpisodeFilterType episodeFilterType) {
        List<EventDto> events = apiDao.searchForEvents(feedAlias, eventTypes, from, to,
                updatedAfter, limit, severities, sortOrder, bbox, episodeFilterType);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        Feature[] features = events.stream()
                .map(EventDtoConverter::convertGeoJson)
                .flatMap(List::stream)
                .toArray(Feature[]::new);
        FeatureCollection fc = new FeatureCollection(features);
        GeoJsonPaginationDTO paginationDTO = new GeoJsonPaginationDTO(fc, events.get(events.size() - 1).getUpdatedAt());
        return Optional.of(paginationDTO);
    }
}
