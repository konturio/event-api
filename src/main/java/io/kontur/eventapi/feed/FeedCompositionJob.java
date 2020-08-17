package io.kontur.eventapi.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;

import java.util.*;

@Component
public class FeedCompositionJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(FeedCompositionJob.class);

    private final KonturEventsDao eventsDao;
    private final FeedDao feedDao;
    private final NormalizedObservationsDao observationsDao;

    public FeedCompositionJob(KonturEventsDao eventsDao, FeedDao feedDao,
                              NormalizedObservationsDao observationsDao) {
        this.eventsDao = eventsDao;
        this.feedDao = feedDao;
        this.observationsDao = observationsDao;
    }

    @Override
    public void run() {
        LOG.info("Feed Composition job has started.");
        List<FeedDto> feeds = feedDao.getFeeds();
        feeds.forEach(this::updateFeed);
        LOG.info("Feed Composition job has finished.");
    }

    private void updateFeed(FeedDto feed) {
        List<KonturEventDto> newEventVersions = eventsDao.getNewEventVersionsForFeed(feed.getFeedId());
        newEventVersions.forEach(event -> createFeedData(event, feed));
    }

    private void createFeedData(KonturEventDto event, FeedDto feed) {
        List<NormalizedObservationsDto> observations = observationsDao.getObservations(event.getObservationIds());
        observations.sort(Comparator.comparing(NormalizedObservationsDto::getLoadedAt));

        FeedDataDto feedDto = new FeedDataDto(event.getEventId(), feed.getFeedId(), event.getVersion());
        NormalizedObservationsDto initialObservation = observations.get(0);
        feedDto.setDescription(initialObservation.getDescription());
        feedDto.setName(initialObservation.getName());
//        feedDto.setObservations(event.getObservationIds());  TODO
        observations
                .forEach(observation -> convertObservation(observation)
                    .ifPresent(feedDto::addEpisode));

        feedDao.insertFeedData(feedDto);
    }

    private Optional<FeedEpisodeDto> convertObservation(NormalizedObservationsDto observation) {
        if (observation.getGeometries() == null) {
            return Optional.empty();
        }
        FeedEpisodeDto feedEpisode = new FeedEpisodeDto();
        feedEpisode.setName(observation.getName());
        feedEpisode.setDescription(observation.getDescription());
        feedEpisode.setType(observation.getType());
        feedEpisode.setSeverity(observation.getEventSeverity()); //TODO event severity?
        feedEpisode.setLoadedAt(observation.getLoadedAt());
        feedEpisode.setGeometries(readJson(observation.getGeometries(), FeatureCollection.class));
        return Optional.of(feedEpisode);
    }

    private <T> T readJson(String json, Class<T> clazz) { //TODO create JsonUtil class
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
