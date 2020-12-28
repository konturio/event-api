package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.NormalizedObservationsMapper;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NormalizedObservationsDao {

    private final NormalizedObservationsMapper mapper;
    private final DataLakeDao dataLakeDao;

    public NormalizedObservationsDao(NormalizedObservationsMapper mapper, DataLakeDao dataLakeDao) {
        this.mapper = mapper;
        this.dataLakeDao = dataLakeDao;
    }

    @Transactional
    public int insert(NormalizedObservation observation) {
        dataLakeDao.markAsNormalized(observation.getObservationId());
        return mapper.insert(observation);
    }

    public void markAsRecombined(UUID observationId) {
        mapper.markAsRecombined(observationId);
    }

    public List<NormalizedObservation> getObservationsNotLinkedToEvent() {
        return mapper.getObservationsNotLinkedToEvent();
    }

    public List<NormalizedObservation> getObservations(List<UUID> observationIds) {
        return observationIds.isEmpty() ? List.of() : mapper.getObservations(observationIds);
    }

    public List<NormalizedObservation> getObservationsByEventId(UUID eventId) {
        return mapper.getObservationsByEventId(eventId);
    }

    public Optional<NormalizedObservation> getDuplicateObservation(OffsetDateTime loadedAt, String externalEpisodeId, UUID observationId, String provider) {
        return mapper.getDuplicateObservation(loadedAt, externalEpisodeId, observationId, provider);
    }

    public Optional<NormalizedObservation> getNormalizedObservationByExternalEpisodeIdAndProvider(String externalEpisodeId, String provider) {
        return mapper.getNormalizedObservationByExternalEpisodeIdAndProvider(externalEpisodeId, provider);
    }
}
