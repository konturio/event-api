package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.KonturEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<KonturEvent> findClosestEventsToObservations(@Param("observationIds") Set<UUID> observationIds,
                                                      @Param("eventIds") Set<UUID> eventIds);

    Set<UUID> getEventsForRolloutEpisodes(@Param("feedId") UUID feedId);

    Integer getNotComposedEventsCount();

    Integer getNotComposedEventsCountForFeeds(@Param("aliases") List<String> aliases);
}
