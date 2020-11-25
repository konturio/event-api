package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.entity.KonturEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KonturEventsDao {

    private static final Logger LOG = LoggerFactory.getLogger(KonturEventsMapper.class);

    private final KonturEventsMapper mapper;

    @Autowired
    public KonturEventsDao(KonturEventsMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<KonturEvent> getEventByExternalId(String externalId) {
        try {
            return mapper.getEventByExternalId(externalId);
        } catch (Exception e) {
            LOG.warn("externalId = {}", externalId);
            LOG.warn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Optional<KonturEvent> getEventWithClosestObservation(OffsetDateTime updatedAt, String geometry, List<String> providers){
        return mapper.getEventWithClosestObservation(updatedAt, geometry, providers);
    }

    @Transactional
    public void insertEvent(KonturEvent event) {
        List<UUID> observationIds = getNewObservations(event);
        observationIds.stream().forEach(obs -> mapper.insert(event.getEventId(), obs));
    }

    private List<UUID> getNewObservations(KonturEvent event) {
        List<UUID> observationIds = new ArrayList<>(event.getObservationIds());
        Optional<KonturEvent> existingEvent = mapper.getEventById(event.getEventId());
        existingEvent.ifPresent(e -> observationIds.removeAll(e.getObservationIds()));
        return observationIds;
    }

    public List<KonturEvent> getEventsForRolloutEpisodes(UUID feedId) {
        return mapper.getEventsForRolloutEpisodes(feedId);
    }
}
