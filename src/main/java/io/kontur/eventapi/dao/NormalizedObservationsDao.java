package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.NormalizedObservationsMapper;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NormalizedObservationsDao {

    private final NormalizedObservationsMapper mapper;

    public NormalizedObservationsDao(NormalizedObservationsMapper mapper) {
        this.mapper = mapper;
    }

    public int insert(NormalizedObservation record) {
        return mapper.insert(record);
    }

    public List<String> getExternalIdsToUpdate() {
        return mapper.getExternalIdsToUpdate();
    }

    public List<NormalizedObservation> getNotCombinedObservationsByExternalId(String externalId) {
        return mapper.getNotCombinedObservationsByExternalId(externalId);
    }

    public List<NormalizedObservation> getObservations(List<UUID> observationIds) {
        return mapper.getObservations(observationIds);
    }

    public Optional<NormalizedObservation> getDuplicateObservation(OffsetDateTime sourceUpdatedAt, String externalEpisodeId, UUID observationId){
        return mapper.getDuplicateObservation(sourceUpdatedAt, externalEpisodeId, observationId);
    }

}
