package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.KonturEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface KonturEventsMapper {

    int insert(@Param("eventId") UUID eventId, @Param("observationId") UUID observationId);

    Optional<KonturEvent> getEventByExternalId(String externalId);

    Optional<KonturEvent> getEventById(UUID evenId);

    Optional<KonturEvent> getEventWithClosestObservation(OffsetDateTime updatedAt, String geometry, List<String> providers);

    Set<UUID> getEventsForRolloutEpisodes(UUID feedId);
}
