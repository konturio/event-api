package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.CombinedAreaDto;
import io.kontur.eventapi.dto.CombinedEpisodeDto;
import io.kontur.eventapi.dto.CombinedEventDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CombinedEventsMapper {

    List<CombinedEventDto> getEventForExternalId(String externalId);

    void insertEvent(CombinedEventDto eventDto);

    void updateEvent(CombinedEventDto eventDto);

    void insertEpisode(CombinedEpisodeDto episodeDto);

    void insertArea(CombinedAreaDto areaDto);
}
