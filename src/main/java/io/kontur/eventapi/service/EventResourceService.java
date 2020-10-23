package io.kontur.eventapi.service;

import io.kontur.eventapi.converter.EventDtoConverter;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EventDto;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventResourceService {

    private final FeedDao feedDao;
    private final DataLakeDao dataLakeDao;

    public EventResourceService(FeedDao feedDao, DataLakeDao dataLakeDao) {
        this.feedDao = feedDao;
        this.dataLakeDao = dataLakeDao;
    }

    public List<EventDto> searchEvents(String feedAlias, List<EventType> eventTypes,
                                       OffsetDateTime after, int limit, List<Severity> severities, SortOrder sortOrder) {
        List<FeedData> feedData = feedDao.searchForEvents(feedAlias, eventTypes, after, limit, severities, sortOrder);

        return feedData.stream()
                .map(EventDtoConverter::convert)
                .collect(Collectors.toList());
    }

    public String getRawData(UUID observationId) {
        DataLake dataLake = dataLakeDao.getDataLakeByObservationId(observationId);
        if (dataLake == null) {
            return null;
        }
        return dataLake.getData();
    }

    public Optional<FeedData> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version) {
        return feedDao.getEventByEventIdAndByVersionOrLast(eventId, feed, version);
    }
}
