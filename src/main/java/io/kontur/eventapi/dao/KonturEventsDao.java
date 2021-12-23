package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public List<KonturEvent> findClosestEventsToObservations(Set<UUID> observationIds, Set<UUID> eventIds) {
        return mapper.findClosestEventsToObservations(observationIds, eventIds);
    }

    @Transactional
    public void appendObservationIntoEvent(UUID eventId, NormalizedObservation observation) {
        feedEventStatusDao.markAsNonActual(observation.getProvider(), eventId);
        mapper.insert(eventId, observation.getObservationId(), observation.getProvider());
        observationsDao.markAsRecombined(observation.getObservationId());
    }

    public Set<UUID> getEventsForRolloutEpisodes(UUID feedId) {
        return mapper.getEventsForRolloutEpisodes(feedId);
    }

    public Integer getNotComposedEventsCount() {
        return mapper.getNotComposedEventsCount();
    }
}
