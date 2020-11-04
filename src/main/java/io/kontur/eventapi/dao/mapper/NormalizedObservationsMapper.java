package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.NormalizedObservation;
import org.apache.ibatis.annotations.Mapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface NormalizedObservationsMapper {

    int insert(NormalizedObservation record);

    List<String> getExternalIdsToUpdate();

    List<NormalizedObservation> getNotCombinedObservationsByExternalId(String externalId);

    List<NormalizedObservation> getObservations(List<UUID> observationIds);

    Optional<NormalizedObservation> getDuplicateObservation(OffsetDateTime loadedAt, String externalEpisodeId, UUID observationId);
}
