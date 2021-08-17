package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface NormalizedObservationsMapper {

    int insert(UUID observationId, String externalEventId, String externalEpisodeId, String provider, String point,
               String geometries, Severity eventSeverity, String name, String description, String episodeDescription,
               EventType type, Boolean active, BigDecimal cost, String region, OffsetDateTime loadedAt,
               OffsetDateTime startedAt, OffsetDateTime endedAt, OffsetDateTime sourceUpdatedAt,
               String sourceUri, boolean recombined);

    void markAsRecombined(UUID observationId);

    List<NormalizedObservation> getObservationsNotLinkedToEvent();

    List<NormalizedObservation> getObservations(List<UUID> observationIds);

    List<NormalizedObservation> getObservationsByEventId(UUID eventId);

    Optional<NormalizedObservation> getDuplicateObservation(OffsetDateTime loadedAt, String externalEpisodeId, UUID observationId, String provider);

    Optional<NormalizedObservation> getNormalizedObservationByExternalEpisodeIdAndProvider(String externalEpisodeId, String provider);

    OffsetDateTime getTimestampAtTimezone(LocalDateTime timestamp, String timezone);
}
