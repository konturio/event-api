package io.kontur.eventapi.service;

import io.kontur.eventapi.converter.EventDtoConverter;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dto.EventType;
import io.kontur.eventapi.dto.FeedDataDto;
import io.kontur.eventapi.resource.dto.EventDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventResourceService {

    private final FeedDao feedDao;

    public EventResourceService(FeedDao feedDao) {
        this.feedDao = feedDao;
    }

    public List<EventDto> searchEvents(OffsetDateTime after, OffsetDateTime before, List<BigDecimal> bbox, String geometry,
                                 BigDecimal distance, EventType type, int offset, int limit) {
        List<FeedDataDto> feedDataDtos = feedDao
                .searchForEvents(after, before, bbox, geometry, distance, type, offset, limit);

        return feedDataDtos.stream()
                .map(EventDtoConverter::convert)
                .collect(Collectors.toList());
    }
}
