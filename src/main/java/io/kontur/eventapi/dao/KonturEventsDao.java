package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.entity.KonturEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    public Optional<KonturEvent> getLatestEventByExternalId(String externalId) {
        try {
            return mapper.getLatestEventByExternalId(externalId);
        } catch (Exception e) {
            LOG.warn("externalId = {}", externalId);
            LOG.warn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Optional<KonturEvent> getEventWithClosestObservation(OffsetDateTime startedAt, String geometry){
        return mapper.getEventWithClosestObservation(startedAt, geometry);
    }

    @Transactional
    public void insertEventVersion(KonturEvent event) {
        event.getObservationIds().forEach(obs -> mapper.insert(event.getEventId(), event.getVersion(), obs));
    }

    public List<KonturEvent> getNewEventVersionsForFeed(UUID feedId) {
        return mapper.getNewEventVersionsForFeed(feedId);
    }
}
