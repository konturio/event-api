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

    int insert(@Param("eventId") UUID eventId,
               @Param("observationId") UUID observationId,
               @Param("provider") String provider);

    Optional<KonturEvent> getEventByExternalId(@Param("externalId") String externalId);

    Optional<KonturEvent> getEventById(@Param("eventId") UUID evenId);

    Optional<KonturEvent> getEventWithClosestObservation(@Param("updatedAt") OffsetDateTime updatedAt,
                                                         @Param("geometry") String geometry,
                                                         @Param("providers") List<String> providers,
                                                         @Param("eventIds") Set<UUID> eventIds);

    Set<UUID> getEventsForRolloutEpisodes(@Param("feedId") UUID feedId);

    Integer getFeedCompositionQueueSize();
}
