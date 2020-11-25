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

    public List<NormalizedObservation> getObservationsNotLinkedToEvent() {
        return mapper.getObservationsNotLinkedToEvent();
    }

    public List<NormalizedObservation> getObservations(List<UUID> observationIds) {
        return observationIds.isEmpty() ? List.of() : mapper.getObservations(observationIds);
    }

    public Optional<NormalizedObservation> getDuplicateObservation(OffsetDateTime loadedAt, String externalEpisodeId, UUID observationId, String provider) {
        return mapper.getDuplicateObservation(loadedAt, externalEpisodeId, observationId, provider);
    }

    public Optional<NormalizedObservation> getNormalizedObservationByExternalEpisodeIdAndProvider(String externalEpisodeId, String provider) {
        return mapper.getNormalizedObservationByExternalEpisodeIdAndProvider(externalEpisodeId, provider);
    }
}
