package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class FeedCompositionJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(FeedCompositionJob.class);
    @Value("${scheduler.feedComposition.alias}")
    private String[] alias;

    protected final KonturEventsDao eventsDao;
    protected final FeedDao feedDao;
    private final NormalizedObservationsDao observationsDao;
    private final List<EpisodeCombinator> episodeCombinators;

    public FeedCompositionJob(KonturEventsDao eventsDao, FeedDao feedDao,
                              NormalizedObservationsDao observationsDao, List<EpisodeCombinator> episodeCombinators, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.eventsDao = eventsDao;
        this.feedDao = feedDao;
        this.observationsDao = observationsDao;
        this.episodeCombinators = episodeCombinators;
    }

    @Override
    public void execute() {
        List<Feed> feeds = feedDao.getFeedsByAliases(Arrays.asList(alias));
        feeds.forEach(this::updateFeed);
    }

    @Override
    public String getName() {
        return "feedComposition";
    }

    protected void updateFeed(Feed feed) {
        Set<UUID> eventsIds = eventsDao.getEventsForRolloutEpisodes(feed.getFeedId());
        LOG.info(String.format("%s feed. %s events to compose", feed.getAlias(), eventsIds.size()));
        eventsIds
                .forEach(event -> createFeedData(event, feed));
    }

    protected void createFeedData(UUID eventId, Feed feed) {
        try {
            List<NormalizedObservation> eventObservations = observationsDao.getObservationsByEventId(eventId);
            eventObservations.sort(comparing(NormalizedObservation::getStartedAt)
                    .thenComparing(NormalizedObservation::getLoadedAt));

            Optional<Long> lastFeedDataVersion = feedDao.getLastFeedDataVersion(eventId, feed.getFeedId());
            FeedData feedData = new FeedData(eventId, feed.getFeedId(),
                    lastFeedDataVersion.map(v -> v + 1).orElse(1L));

            feedData.setObservations(
                    eventObservations.stream().map(NormalizedObservation::getObservationId).collect(toSet()));
            fillEpisodes(eventObservations, feedData);
            fillFeedData(feedData);

            feedData.setEnriched(feed.getEnrichment().isEmpty());

            feedDao.insertFeedData(feedData);
        } catch (Exception e) {
            LOG.error(
                    String.format("Error while processing event with id = '%s', for '%s' feed. Error: %s", eventId.toString(),
                            feed.getAlias(), e.getMessage()), e);
        }
    }

    private void fillFeedData(FeedData feedData) {
        List<FeedEpisode> episodes = feedData.getEpisodes();

        feedData.setName(episodes.stream()
                .filter(e -> !isEmpty(e.getName()))
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getName).orElse(null));

        feedData.setProperName(episodes.stream()
                .filter(e -> !isEmpty(e.getProperName()))
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getProperName).orElse(null));

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

        feedData.setUrls(episodes.stream()
                .filter(e -> e.getUrls() != null && !e.getUrls().isEmpty())
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getUrls).orElse(emptyList()));

        feedData.setLocation(episodes.stream()
                .filter(e -> !isEmpty(e.getLocation()))
                .max(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getLocation).orElse(null));
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
        if (!CollectionUtils.isEmpty(feedData.getEpisodes())) {
            EpisodeCombinator episodeCombinator = Applicable.get(episodeCombinators, observations.get(0));
            feedData.setEpisodes(episodeCombinator.postProcessEpisodes(feedData.getEpisodes()));
        }
    }

    private void addEpisode(FeedData feedData, FeedEpisode episode) {
        checkNotNull(episode.getStartedAt());
        checkNotNull(episode.getEndedAt());
        checkState(!episode.getStartedAt().isAfter(episode.getEndedAt()));

        feedData.addEpisode(episode);
    }
}
