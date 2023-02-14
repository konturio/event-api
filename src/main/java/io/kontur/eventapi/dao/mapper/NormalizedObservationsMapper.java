package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Mapper
public interface NormalizedObservationsMapper {

    int insert(@Param("observationId") UUID observationId,
               @Param("externalEventId") String externalEventId,
               @Param("externalEpisodeId") String externalEpisodeId,
               @Param("provider") String provider,
               @Param("origin") String origin,
               @Param("name") String name,
               @Param("properName") String properName,
               @Param("description") String description,
               @Param("episodeDescription") String episodeDescription,
               @Param("type") EventType type,
               @Param("eventSeverity") Severity eventSeverity,
               @Param("active") Boolean active,
               @Param("loadedAt") OffsetDateTime loadedAt,
               @Param("startedAt") OffsetDateTime startedAt,
               @Param("endedAt") OffsetDateTime endedAt,
               @Param("sourceUpdatedAt") OffsetDateTime sourceUpdatedAt,
               @Param("region") String region,
               @Param("urls") List<String> urls,
               @Param("cost") BigDecimal cost,
               @Param("loss") Map<String, Object> loss,
               @Param("point") String point,
               @Param("geometries") String geometries,
               @Param("autoExpire") Boolean autoExpire,
               @Param("recombined") boolean recombined);

    void markAsRecombined(@Param("observationId") UUID observationId);

    List<NormalizedObservation> getObservationsNotLinkedToEvent(@Param("providers") List<String> providers);

    List<NormalizedObservation> getFirmsObservationsNotLinkedToEventFor24Hours();

    List<Set<UUID>> clusterObservationsByGeography(@Param("observationIds") Set<UUID> observationIds);

    List<NormalizedObservation> getObservations(@Param("observationIds") Set<UUID> observationIds);

    List<NormalizedObservation> getObservationsByEventId(@Param("eventId") UUID eventId);

    Optional<NormalizedObservation> getDuplicateObservation(@Param("loadedAt") OffsetDateTime loadedAt,
                                                            @Param("externalEpisodeId") String externalEpisodeId,
                                                            @Param("observationId") UUID observationId,
                                                            @Param("provider") String provider);

    OffsetDateTime getTimestampAtTimezone(@Param("timestamp") LocalDateTime timestamp,
                                          @Param("timezone") String timezone);

    Integer getNotRecombinedObservationsCount();
}
