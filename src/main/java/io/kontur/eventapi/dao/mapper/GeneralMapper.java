package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.ProcessingDuration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

@Mapper
public interface GeneralMapper {

    Optional<ProcessingDuration> getObservationNormalizationDuration(@Param("latestNormalizedAt") OffsetDateTime latestNormalizedAt);

    Optional<ProcessingDuration> getObservationsRecombinationDuration(@Param("latestRecombinedAt") OffsetDateTime latestRecombinedAt);

    Optional<ProcessingDuration> getEventCompositionDuration(@Param("latestComposedAt") OffsetDateTime latestComposedAt);

    Optional<ProcessingDuration> getEventEnrichmentDuration(@Param("latestEnrichedAt") OffsetDateTime latestEnrichedAt);
}
