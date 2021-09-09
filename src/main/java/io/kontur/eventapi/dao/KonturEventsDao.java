package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class KonturEventsDao {

    private static final Logger LOG = LoggerFactory.getLogger(KonturEventsMapper.class);

    private final KonturEventsMapper mapper;
    private final NormalizedObservationsDao observationsDao;
    private final FeedEventStatusDao feedEventStatusDao;

    @Autowired
    public KonturEventsDao(KonturEventsMapper mapper, NormalizedObservationsDao observationsDao, FeedEventStatusDao feedEventStatusDao) {
        this.mapper = mapper;
        this.observationsDao = observationsDao;
        this.feedEventStatusDao = feedEventStatusDao;
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

    public Optional<KonturEvent> getEventWithClosestObservation(OffsetDateTime updatedAt, String geometry, List<String> providers, Set<UUID> eventIds) {
        return mapper.getEventWithClosestObservation(updatedAt, geometry, providers, eventIds);
    }

    @Transactional
    public void appendObservationIntoEvent(KonturEvent event, NormalizedObservation observation) {
        feedEventStatusDao.markAsNonActual(observation.getProvider(), event.getEventId());
        mapper.insert(event.getEventId(), observation.getObservationId(), observation.getProvider());
        observationsDao.markAsRecombined(observation.getObservationId());
    }

    public Set<UUID> getEventsForRolloutEpisodes(UUID feedId) {
        return mapper.getEventsForRolloutEpisodes(feedId);
    }
}
