package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.entity.KonturEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KonturEventsDao {

    private final KonturEventsMapper mapper;

    @Autowired
    public KonturEventsDao(KonturEventsMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<KonturEvent> getLatestEventByExternalId(String externalId) {
        return mapper.getLatestEventByExternalId(externalId);
    }

    @Transactional
    public void insertEventVersion(KonturEvent event) {
        event.getObservationIds().forEach(obs -> mapper.insert(event.getEventId(), event.getVersion(), obs));
    }

    public List<KonturEvent> getNewEventVersionsForFeed(UUID feedId) {
        return mapper.getNewEventVersionsForFeed(feedId);
    }
}
