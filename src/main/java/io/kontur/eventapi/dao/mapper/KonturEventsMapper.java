package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.KonturEventDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface KonturEventsMapper {

    int insert(@Param("eventId") UUID eventId, @Param("version") Long version, @Param("observationId") UUID observationId);

    Optional<KonturEventDto> getLatestEventByExternalId(String externalId);

    List<KonturEventDto> getNewEventVersionsForFeed(UUID feedId);
}
