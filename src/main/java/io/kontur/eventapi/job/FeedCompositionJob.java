package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.job.exception.FeedCompositionSkipException;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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
import static io.kontur.eventapi.entity.Severity.UNKNOWN;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
        if (!CollectionUtils.isEmpty(eventsIds)) {
            LOG.info(format("%s feed. %s events to compose", feed.getAlias(), eventsIds.size()));
            eventsIds
                    .forEach(event -> createFeedData(event, feed));
        }
    }

    @Timed(value = "feedComposition.event.timer")
    @Counted(value = "feedComposition.event.counter")
    protected void createFeedData(UUID eventId, Feed feed) {
        try {
            LOG.info(format("%s feed. Processing event %s", feed.getAlias(), eventId));
            List<NormalizedObservation> eventObservations = observationsDao.getObservationsByEventId(eventId);
            LOG.info(format("%s feed. Got observations for event %s", feed.getAlias(), eventId));
            eventObservations.sort(comparing(NormalizedObservation::getStartedAt, nullsLast(naturalOrder()))
                    .thenComparing(NormalizedObservation::getLoadedAt));
            LOG.info(format("%s feed. Sorted observations for event %s", feed.getAlias(), eventId));
            Optional<Long> lastFeedDataVersion = feedDao.getLastFeedDataVersion(eventId, feed.getFeedId());
            FeedData feedData = new FeedData(eventId, feed.getFeedId(),
                    lastFeedDataVersion.map(v -> v + 1).orElse(1L));

            feedData.setObservations(
                    eventObservations.stream().map(NormalizedObservation::getObservationId).collect(toSet()));
            LOG.info(format("%s feed. Set observation IDs for event %s", feed.getAlias(), eventId));
            fillEpisodes(eventObservations, feedData);
            LOG.info(format("%s feed. Filled episodes for event %s", feed.getAlias(), eventId));
            fillFeedData(feedData, eventObservations);
            LOG.info(format("%s feed. Filled feed data for event %s", feed.getAlias(), eventId));
            feedData.setEnriched(feed.getEnrichment().isEmpty());

            feedDao.insertFeedData(feedData, feed.getAlias());
        } catch (FeedCompositionSkipException fe) {
            LOG.info(format("Skipped processing event: id = '%s', feed = '%s'. Error: %s",
                    eventId.toString(), feed.getAlias(), fe.getMessage()));
        } catch (Exception e) {
            LOG.error(format("Error while processing event: id = '%s', feed = '%s'. Error: %s", eventId.toString(),
                            feed.getAlias(), e.getMessage()), e);
        }
    }

    private void fillFeedData(FeedData feedData, List<NormalizedObservation> eventObservations) {
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

        feedData.setSeverity(episodes.stream()
                .map(FeedEpisode::getSeverity)
                .filter(Objects::nonNull)
                .max(comparing(Severity::getValue))
                .orElse(UNKNOWN));

        feedData.setType(episodes.stream()
                .filter(ep -> ep.getType() != null)
                .max(comparing(FeedEpisode::getUpdatedAt))
                .map(FeedEpisode::getType).orElse(null));

        Map<String, Object> loss = new HashMap<>();
        episodes.stream()
                .sorted(comparing(FeedEpisode::getSourceUpdatedAt))
                .forEachOrdered(obs -> obs.getLoss().entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .forEach(e -> loss.put(e.getKey(), e.getValue())));
        feedData.setLoss(loss);

        feedData.setActive(eventObservations.stream()
                .filter(ep -> ep.getActive() != null)
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(NormalizedObservation::getActive).orElse(null));

        feedData.setAutoExpire(eventObservations.stream()
                .filter(obs -> obs.getAutoExpire() != null)
                .max(comparing(NormalizedObservation::getSourceUpdatedAt))
                .map(NormalizedObservation::getAutoExpire)
                .orElse(null));
    }

    private void fillEpisodes(List<NormalizedObservation> observations, FeedData feedData) {
        LOG.info(format("Episodes processing for event %s", feedData.getEventId()));
        final Set<NormalizedObservation> observationSet = new HashSet<>(observations);
        LOG.info(format("Copied observations for event %s", feedData.getEventId()));
        observations.forEach(observation -> {
            EpisodeCombinator episodeCombinator = Applicable.get(episodeCombinators, observation);
            episodeCombinator.processObservation(observation, feedData, observationSet)
                    .forEach(episode -> {
                        if (episode.getStartedAt() != null && episode.getEndedAt() != null
                                && episode.getStartedAt().isAfter(episode.getEndedAt())) {
                            OffsetDateTime startedAt = episode.getStartedAt();
                            episode.setStartedAt(episode.getEndedAt());
                            addEpisode(feedData, episode);

                            FeedEpisode newEpisode = new FeedEpisode();
                            BeanUtils.copyProperties(episode, newEpisode);
                            newEpisode.setStartedAt(startedAt);
                            newEpisode.setEndedAt(startedAt);
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
