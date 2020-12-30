package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.firms.FirmsUtil;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FeedCompositionJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(FeedCompositionJob.class);

    private final KonturEventsDao eventsDao;
    private final FeedDao feedDao;
    private final NormalizedObservationsDao observationsDao;
    private final List<EpisodeCombinator> episodeCombinators;

    public FeedCompositionJob(KonturEventsDao eventsDao, FeedDao feedDao,
                              NormalizedObservationsDao observationsDao, List<EpisodeCombinator> episodeCombinators) {
        this.eventsDao = eventsDao;
        this.feedDao = feedDao;
        this.observationsDao = observationsDao;
        this.episodeCombinators = episodeCombinators;
    }

    @Override
    @Counted(value = "job.feed_composition.counter")
    @Timed(value = "job.feed_composition.in_progress_timer", longTask = true)
    public void run() {
        LOG.info("Feed Composition job has started.");
        List<Feed> feeds = feedDao.getFeeds();
        feeds.forEach(this::updateFeed);
        LOG.info("Feed Composition job has finished.");
    }

    private void updateFeed(Feed feed) {
        Set<UUID> eventsIds = eventsDao.getEventsForRolloutEpisodes(feed.getFeedId());
        LOG.info(String.format("%s feed. %s events to compose", feed.getAlias(), eventsIds.size()));
        eventsIds.forEach(event -> createFeedData(event, feed));
    }

    private void createFeedData(UUID eventId, Feed feed) {
        List<NormalizedObservation> eventObservations = observationsDao.getObservationsByEventId(eventId);
        eventObservations.sort(Comparator.comparing(NormalizedObservation::getLoadedAt));

        Optional<FeedData> lastFeedData = feedDao.getLastFeedData(eventId, feed.getFeedId());
        FeedData feedData = new FeedData(eventId, feed.getFeedId(), lastFeedData.map(f -> f.getVersion() + 1).orElse(1L));

        fillFeedData(feedData, eventObservations);
        fillEpisodes(eventObservations, feedData);

        overrideFirmsFeedDataFields(eventObservations, feedData);

        feedDao.insertFeedData(feedData);
    }

    private void overrideFirmsFeedDataFields(List<NormalizedObservation> eventObservations, FeedData feedData) {
        if (!eventObservations.isEmpty() && FirmsUtil.FIRMS_PROVIDERS.contains(eventObservations.get(0).getProvider())){
            feedData.setName(feedData.getEpisodes().stream().max(Comparator.comparing(FeedEpisode::getEndedAt)).get().getName());
            feedData.setStartedAt(feedData.getEpisodes().stream().map(FeedEpisode::getStartedAt).min(OffsetDateTime::compareTo).get());
            feedData.setEndedAt(feedData.getEpisodes().stream().map(FeedEpisode::getEndedAt).max(OffsetDateTime::compareTo).get());
        }
    }

    private void fillEpisodes(List<NormalizedObservation> observations, FeedData feedData) {
        observations.forEach(observation -> {
            EpisodeCombinator episodeCombinator = Applicable.get(episodeCombinators, observation);
            Optional<FeedEpisode> feedEpisode = episodeCombinator.processObservation(observation, feedData, Set.copyOf(observations));
            feedEpisode.ifPresent(feedData::addEpisode);
        });
    }

    private void fillFeedData(FeedData feedDto, List<NormalizedObservation> observations) {
        feedDto.setObservations(observations
                .stream()
                .map(NormalizedObservation::getObservationId)
                .collect(Collectors.toList()));

        ListIterator<NormalizedObservation> iterator = observations.listIterator(observations.size());
        while (iterator.hasPrevious()) {
            boolean isDataFilled = true;
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

}
