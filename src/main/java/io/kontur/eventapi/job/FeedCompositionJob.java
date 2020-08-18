package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wololo.geojson.FeatureCollection;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

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
        observations.sort(Comparator.comparing(NormalizedObservationsDto::getUpdatedAt));

        FeedDataDto feedDto = new FeedDataDto(event.getEventId(), feed.getFeedId(), event.getVersion());

        fillFeedData(feedDto, observations);

        observations.forEach(observation -> convertObservation(observation)
                .ifPresent(feedDto::addEpisode));

        feedDao.insertFeedData(feedDto);
    }

    private void fillFeedData(FeedDataDto feedDto, List<NormalizedObservationsDto> observations) {
        boolean isDataFilled = true;
        ListIterator<NormalizedObservationsDto> iterator = observations.listIterator(observations.size());
        while (iterator.hasPrevious()) {
            NormalizedObservationsDto observation = iterator.previous();

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
                if (observation.getUpdatedAt() != null) {
                    feedDto.setUpdatedAt(observation.getUpdatedAt());
                } else {
                    isDataFilled = false;
                }
            }
            //        feedDto.setObservations(event.getObservationIds());  TODO

            if (isDataFilled) {
                break;
            }
        }
    }

    private Optional<FeedEpisodeDto> convertObservation(NormalizedObservationsDto observation) {
        if (observation.getGeometries() == null) {
            return Optional.empty();
        }
        FeedEpisodeDto feedEpisode = new FeedEpisodeDto();
        feedEpisode.setName(observation.getName());
        feedEpisode.setDescription(observation.getEpisodeDescription());
        feedEpisode.setType(observation.getType());
        feedEpisode.setActive(observation.getActive());
        feedEpisode.setSeverity(observation.getEventSeverity());
        feedEpisode.setStartedAt(observation.getStartedAt());
        feedEpisode.setEndedAt(observation.getEndedAt());
        feedEpisode.setUpdatedAt(observation.getUpdatedAt());
        feedEpisode.setGeometries(readJson(observation.getGeometries(), FeatureCollection.class));
        return Optional.of(feedEpisode);
    }

}
