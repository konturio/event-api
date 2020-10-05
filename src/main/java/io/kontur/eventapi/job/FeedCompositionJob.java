package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wololo.geojson.FeatureCollection;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.kontur.eventapi.util.JsonUtil.readJson;

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
        List<Feed> feeds = feedDao.getFeeds();
        feeds.forEach(this::updateFeed);
        LOG.info("Feed Composition job has finished.");
    }

    private void updateFeed(Feed feed) {
        List<KonturEvent> newEventVersions = eventsDao.getNewEventVersionsForFeed(feed.getFeedId());
        newEventVersions.forEach(event -> createFeedData(event, feed));
    }

    private void createFeedData(KonturEvent event, Feed feed) {
        List<NormalizedObservation> observations = observationsDao.getObservations(event.getObservationIds());
        observations.sort(Comparator.comparing(NormalizedObservation::getSourceUpdatedAt));

        FeedData feedDto = new FeedData(event.getEventId(), feed.getFeedId(), event.getVersion());

        fillFeedData(feedDto, observations);

        observations.forEach(observation -> convertObservation(observation)
                .ifPresent(feedDto::addEpisode));

        feedDao.insertFeedData(feedDto);
    }

    private void fillFeedData(FeedData feedDto, List<NormalizedObservation> observations) {
        boolean isDataFilled = true;

        feedDto.setObservations(observations
                .stream()
                .map(NormalizedObservation::getObservationId)
                .collect(Collectors.toList()));

        ListIterator<NormalizedObservation> iterator = observations.listIterator(observations.size());
        while (iterator.hasPrevious()) {
            NormalizedObservation observation = iterator.previous();

            if (StringUtils.isEmpty(feedDto.getDescription())) {
                if (!StringUtils.isEmpty(observation.getDescription())) {
                    feedDto.setDescription(observation.getDescription());
                } else {
                    isDataFilled = false;
                }
            }
            if (StringUtils.isEmpty(feedDto.getName())) {
                if (!StringUtils.isEmpty(observation.getName())) {
                    feedDto.setName(observation.getName());
                } else {
                    isDataFilled = false;
                }
            }
            if (feedDto.getStartedAt() == null) {
                if (observation.getStartedAt() != null) {
                    feedDto.setStartedAt(observation.getStartedAt());
                } else {
                    isDataFilled = false;
                }
            }
            if (feedDto.getEndedAt() == null) {
                if (observation.getEndedAt() != null) {
                    feedDto.setEndedAt(observation.getEndedAt());
                } else {
                    isDataFilled = false;
                }
            }
            if (feedDto.getUpdatedAt() == null) {
                if (observation.getLoadedAt() != null) {
                    feedDto.setUpdatedAt(observation.getLoadedAt());
                } else {
                    isDataFilled = false;
                }
            }

            if (isDataFilled) {
                break;
            }
        }
    }

    private Optional<FeedEpisode> convertObservation(NormalizedObservation observation) {
        if (observation.getGeometries() == null) {
            return Optional.empty();
        }
        FeedEpisode feedEpisode = new FeedEpisode();
        feedEpisode.setName(observation.getName());
        feedEpisode.setDescription(observation.getEpisodeDescription());
        feedEpisode.setType(observation.getType());
        feedEpisode.setActive(observation.getActive());
        feedEpisode.setSeverity(observation.getEventSeverity());
        feedEpisode.setStartedAt(observation.getStartedAt());
        feedEpisode.setEndedAt(observation.getEndedAt());
        feedEpisode.setUpdatedAt(observation.getLoadedAt());
        feedEpisode.setSourceUpdatedAt(observation.getSourceUpdatedAt());
        feedEpisode.setGeometries(readJson(observation.getGeometries(), FeatureCollection.class));
        return Optional.of(feedEpisode);
    }

}
