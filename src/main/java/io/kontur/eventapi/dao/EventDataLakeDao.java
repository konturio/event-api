package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.EventDataLakeMapper;
import io.kontur.eventapi.dto.EventDataLakeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EventDataLakeDao { //TODO rename to DataLakeDao

    private final EventDataLakeMapper mapper;

    @Autowired
    public EventDataLakeDao(EventDataLakeMapper mapper) {
        this.mapper = mapper;
    }

    public void storeEventData(EventDataLakeDto eventDataLakeDto) {
        mapper.create(eventDataLakeDto);
    }

    public Optional<EventDataLakeDto> getLatestUpdatedHazard(String provider) {
        return mapper.getLatestUpdatedEventForProvider(provider);
    }

    public List<String> getPdcEventsWithoutAreas() {
        return mapper.getPdcHazardsWithoutAreas();
    }

    public List<EventDataLakeDto> getDenormalizedEvents() {
        return mapper.getDenormalizedEvents();
    }
}
