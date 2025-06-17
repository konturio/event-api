package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.NormalizedObservationsMapper;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class NormalizedObservationsDao {

    private final NormalizedObservationsMapper mapper;
    private final DataLakeDao dataLakeDao;

    public NormalizedObservationsDao(NormalizedObservationsMapper mapper, DataLakeDao dataLakeDao) {
        this.mapper = mapper;
        this.dataLakeDao = dataLakeDao;
    }

    @Transactional
    public int insert(NormalizedObservation obs) {
        dataLakeDao.markAsNormalized(obs.getObservationId());
        String geometries = writeJson(obs.getGeometries());
        return mapper.insert(obs.getObservationId(), obs.getExternalEventId(), obs.getExternalEpisodeId(),
                obs.getProvider(), obs.getOrigin(), obs.getName(), obs.getProperName(), obs.getDescription(),
                obs.getEpisodeDescription(), obs.getType(), obs.getEventSeverity(), obs.getActive(), obs.getLoadedAt(),
                obs.getStartedAt(), obs.getEndedAt(), obs.getSourceUpdatedAt(), obs.getRegion(), obs.getUrls(),
                obs.getCost(), obs.getLoss(), obs.getSeverityData(), geometries, obs.getAutoExpire(), obs.getRecombined());
    }

    public void markAsRecombined(UUID observationId) {
        mapper.markAsRecombined(observationId);
    }

    public List<NormalizedObservation> getObservationsNotLinkedToEvent(List<String> providers) {
        return mapper.getObservationsNotLinkedToEvent(providers);
    }

    public List<NormalizedObservation> getFirmsObservationsNotLinkedToEventFor24Hours() {
        return mapper.getFirmsObservationsNotLinkedToEventFor24Hours();
    }

    public List<Set<UUID>> clusterObservationsByGeography(Set<UUID> observationIds) {
        return mapper.clusterObservationsByGeography(observationIds);
    }

    public List<NormalizedObservation> getObservations(Set<UUID> observationIds) {
        return observationIds.isEmpty() ? List.of() : mapper.getObservations(observationIds);
    }

    public List<NormalizedObservation> getObservationsByEventId(UUID eventId) {
        return mapper.getObservationsByEventId(eventId);
    }

    public Optional<NormalizedObservation> getDuplicateObservation(OffsetDateTime loadedAt, String externalEpisodeId, UUID observationId, String provider) {
        return mapper.getDuplicateObservation(loadedAt, externalEpisodeId, observationId, provider);
    }

    public OffsetDateTime getTimestampAtTimezone(LocalDateTime timestamp, String timezone) {
        return mapper.getTimestampAtTimezone(timestamp, timezone);
    }

    public Integer getNotRecombinedObservationsCount() {
        return mapper.getNotRecombinedObservationsCount();
    }
}
