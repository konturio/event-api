package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class FeedCompositionJob extends AbstractJob {

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
    public void execute() {
        List<Feed> feeds = feedDao.getFeeds();
        feeds.forEach(this::updateFeed);
    }

    private void updateFeed(Feed feed) {
        Set<UUID> eventsIds = eventsDao.getEventsForRolloutEpisodes(feed.getFeedId());
        LOG.info(String.format("%s feed. %s events to compose", feed.getAlias(), eventsIds.size()));
        eventsIds.forEach(event -> createFeedData(event, feed));
    }

    private void createFeedData(UUID eventId, Feed feed) {
        List<NormalizedObservation> eventObservations = observationsDao.getObservationsByEventId(eventId);
        eventObservations.sort(comparing(NormalizedObservation::getStartedAt)
                .thenComparing(NormalizedObservation::getLoadedAt));

        Optional<FeedData> lastFeedData = feedDao.getLastFeedData(eventId, feed.getFeedId());
        FeedData feedData = new FeedData(eventId, feed.getFeedId(), lastFeedData.map(f -> f.getVersion() + 1).orElse(1L));

        feedData.setObservations(eventObservations.stream().map(NormalizedObservation::getObservationId).collect(toList()));
        fillEpisodes(eventObservations, feedData);
        fillFeedData(feedData);

        feedDao.insertFeedData(feedData);
    }

    private void fillFeedData(FeedData feedData) {
        List<FeedEpisode> episodes = feedData.getEpisodes();

        feedData.setName(episodes.stream()
                .filter(e -> !isEmpty(e.getName()))
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getName).orElse(null));

        feedData.setDescription(episodes.stream()
                .filter(e -> !isEmpty(e.getDescription()))
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getDescription).orElse(null));

        feedData.setStartedAt(episodes.stream()
                .min(comparing(FeedEpisode::getStartedAt))
                .map(FeedEpisode::getStartedAt).orElse(null));

        feedData.setEndedAt(episodes.stream()
                .max(comparing(FeedEpisode::getEndedAt))
                .map(FeedEpisode::getEndedAt).orElse(null));

        feedData.setUpdatedAt(episodes.stream()
                .max(comparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getUpdatedAt).orElse(null));
    }

    private void fillEpisodes(List<NormalizedObservation> observations, FeedData feedData) {
        observations.forEach(observation -> {
            EpisodeCombinator episodeCombinator = Applicable.get(episodeCombinators, observation);
            Optional<FeedEpisode> feedEpisode = episodeCombinator.processObservation(observation, feedData, Set.copyOf(observations));
            feedEpisode.ifPresent(episode -> {
                if (episode.getStartedAt().isAfter(episode.getEndedAt())) {
                    OffsetDateTime endedAt = episode.getEndedAt();
                    episode.setEndedAt(episode.getStartedAt());
                    addEpisode(feedData, episode);

                    FeedEpisode newEpisode = new FeedEpisode();
                    BeanUtils.copyProperties(episode, newEpisode);
                    newEpisode.setStartedAt(endedAt);
                    newEpisode.setEndedAt(endedAt);
                    addEpisode(feedData, newEpisode);
                } else {
                    addEpisode(feedData, episode);
                }
            });
        });
    }

    private void addEpisode(FeedData feedData, FeedEpisode episode) {
        checkNotNull(episode.getStartedAt());
        checkNotNull(episode.getEndedAt());
        checkState(!episode.getStartedAt().isAfter(episode.getEndedAt()));

        feedData.addEpisode(episode);
    }
}
