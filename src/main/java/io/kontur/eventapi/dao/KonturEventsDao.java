package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.entity.KonturEvent;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.util.GeometryUtil;
import org.wololo.geojson.FeatureCollection;
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

    private static final double USA_MIN_LON = -170.0;
    private static final double USA_MIN_LAT = 14.0;
    private static final double USA_MAX_LON = -66.0;
    private static final double USA_MAX_LAT = 72.0;

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
        if (outsideUsa(observation.getGeometries())) {
            feedEventStatusDao.markAsNonActualExcludeFeed(observation.getProvider(), eventId, "micglobal");
        } else {
            feedEventStatusDao.markAsNonActual(observation.getProvider(), eventId);
        }
        mapper.insert(eventId, observation.getObservationId(), observation.getProvider());
        observationsDao.markAsRecombined(observation.getObservationId());
    }

    private boolean outsideUsa(FeatureCollection geometries) {
        return geometries != null && !GeometryUtil.intersectsEnvelope(geometries,
                USA_MIN_LON, USA_MIN_LAT, USA_MAX_LON, USA_MAX_LAT);
    }

    public Set<UUID> getEventsForRolloutEpisodes(UUID feedId) {
        return mapper.getEventsForRolloutEpisodes(feedId);
    }

    public Integer getNotComposedEventsCount() {
        return mapper.getNotComposedEventsCount();
    }
}
