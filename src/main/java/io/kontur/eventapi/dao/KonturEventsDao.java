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
import java.util.ArrayList;
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
    private final FeedDao feedDao;

    @Autowired
    public KonturEventsDao(KonturEventsMapper mapper, NormalizedObservationsDao observationsDao, FeedEventStatusDao feedEventStatusDao, FeedDao feedDao) {
        this.mapper = mapper;
        this.observationsDao = observationsDao;
        this.feedEventStatusDao = feedEventStatusDao;
        this.feedDao = feedDao;
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

    public Optional<KonturEvent> getEventWithClosestObservation(OffsetDateTime updatedAt, String geometry, List<String> providers) {
        return mapper.getEventWithClosestObservation(updatedAt, geometry, providers);
    }

    @Transactional
    public void insertEvent(KonturEvent event) {
        List<UUID> notLinkedObservationIds = getNewObservations(event);

        if (!notLinkedObservationIds.isEmpty()) {
            NormalizedObservation observation = observationsDao.getObservations(notLinkedObservationIds).get(0);

            feedDao.getFeeds().stream()
                    .filter(f -> f.getProviders().contains(observation.getProvider()))
                    .forEach(f -> feedEventStatusDao.markAsActual(f.getFeedId(), event.getEventId(), false));
        }

        notLinkedObservationIds.forEach(observationId -> {
            mapper.insert(event.getEventId(), observationId);
            observationsDao.markAsRecombined(observationId);
        });
    }

    private List<UUID> getNewObservations(KonturEvent event) {
        List<UUID> observationIds = new ArrayList<>(event.getObservationIds());
        Optional<KonturEvent> existingEvent = mapper.getEventById(event.getEventId());
        existingEvent.ifPresent(e -> observationIds.removeAll(e.getObservationIds()));
        return observationIds;
    }

    public Set<UUID> getEventsForRolloutEpisodes(UUID feedId) {
        return mapper.getEventsForRolloutEpisodes(feedId);
    }
}
