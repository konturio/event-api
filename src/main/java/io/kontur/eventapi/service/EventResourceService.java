package io.kontur.eventapi.service;

import io.kontur.eventapi.converter.EventDtoConverter;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.EventDto;
import io.kontur.eventapi.resource.dto.GeoJsonPaginationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventResourceService {

    private final FeedDao feedDao;
    private final DataLakeDao dataLakeDao;
    private final static Logger LOG = LoggerFactory.getLogger(EventResourceService.class);

    public EventResourceService(FeedDao feedDao, DataLakeDao dataLakeDao) {
        this.feedDao = feedDao;
        this.dataLakeDao = dataLakeDao;
    }

    @Cacheable(cacheResolver = "cacheResolver")
    public List<EventDto> searchEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from, OffsetDateTime to,
                                       OffsetDateTime updatedAfter, int limit, List<Severity> severities, SortOrder sortOrder,
                                       List<BigDecimal> bbox, EpisodeFilterType episodeFilterType) {
        LOG.debug("Start searchEvents DB call");
        List<FeedData> feedData = feedDao.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities,
                sortOrder, bbox, episodeFilterType);
        LOG.debug("Finished searchEvents DB call");
        LOG.debug("Process result");
        return feedData.stream()
                .map(EventDtoConverter::convert)
                .collect(Collectors.toList());
    }

    public Optional<GeoJsonPaginationDTO> searchEventsGeoJson(String feedAlias, List<EventType> eventTypes,
                                                              OffsetDateTime from, OffsetDateTime to,
                                                              OffsetDateTime updatedAfter, int limit,
                                                              List<Severity> severities, SortOrder sortOrder,
                                                              List<BigDecimal> bbox, EpisodeFilterType episodeFilterType) {
        List<FeedData> events = feedDao.searchForEvents(feedAlias, eventTypes, from, to,
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

    public String getRawData(UUID observationId) {
        DataLake dataLake = dataLakeDao.getDataLakeByObservationId(observationId);
        if (dataLake == null) {
            return null;
        }
        return dataLake.getData();
    }

    public Optional<EventDto> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version) {
        return feedDao.getEventByEventIdAndByVersionOrLast(eventId, feed, version).map(EventDtoConverter::convert);
    }

    public List<Feed> getFeeds() {
        return feedDao.getFeeds();
    }
}
