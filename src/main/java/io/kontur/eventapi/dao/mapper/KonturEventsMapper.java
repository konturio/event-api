package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.KonturEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface KonturEventsMapper {

    int insert(@Param("eventId") UUID eventId, @Param("version") Long version, @Param("observationId") UUID observationId);

    Optional<KonturEvent> getLatestEventByExternalId(String externalId);

    Optional<KonturEvent> getEventWithClosestObservation(OffsetDateTime startedAt, String geometry);

    List<KonturEvent> getNewEventVersionsForFeed(UUID feedId);
}
