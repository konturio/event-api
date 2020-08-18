package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.EventDataLakeDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EventDataLakeMapper {

    void create(EventDataLakeDto eventDataLakeDto);

    Optional<EventDataLakeDto> getLatestUpdatedEventForProvider(String provider);

    List<String> getPdcHazardsWithoutAreas();

    List<EventDataLakeDto> getDenormalizedEvents();
}
