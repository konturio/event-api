package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.KonturEventDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface KonturEventsMapper {

    int insert(KonturEventDto eventDto);

    Optional<KonturEventDto> getLatestEventByExternalId(String externalId);

}
