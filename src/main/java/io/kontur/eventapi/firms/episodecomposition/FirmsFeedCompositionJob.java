package io.kontur.eventapi.firms.episodecomposition;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;


@Component
public class FirmsFeedCompositionJob extends FeedCompositionJob {

    private static final Logger LOG = LoggerFactory.getLogger(FirmsFeedCompositionJob.class);
    @Value("${scheduler.feedComposition.firmsAlias}")
    private String[] alias;
    private final ExecutorService executor;

    public FirmsFeedCompositionJob(KonturEventsDao eventsDao, FeedDao feedDao, NormalizedObservationsDao observationsDao,
                                   List<EpisodeCombinator> episodeCombinators, MeterRegistry meterRegistry,
                                   ExecutorService firmsFeedCompositionExecutor) {
        super(eventsDao, feedDao, observationsDao, episodeCombinators, meterRegistry);
        this.executor = firmsFeedCompositionExecutor;
    }

    @Override
    public void execute() {
        List<Feed> feeds = feedDao.getFeedsByAliases(Arrays.asList(alias));
        feeds.forEach(this::updateFeed);
    }

    @Override
    protected void updateFeed(Feed feed) {
        try {
            Set<UUID> eventsIds = eventsDao.getEventsForRolloutEpisodes(feed.getFeedId());
            if (!CollectionUtils.isEmpty(eventsIds)) {
                LOG.info(String.format("%s feed. %s events to compose", feed.getAlias(), eventsIds.size()));
                CompletableFuture<?>[] tasks = eventsIds.stream()
                        .map(event -> runAsync(() -> createFeedData(event, feed), executor))
                        .toArray(CompletableFuture[]::new);
                allOf(tasks).join();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "firmsFeedComposition";
    }
}
