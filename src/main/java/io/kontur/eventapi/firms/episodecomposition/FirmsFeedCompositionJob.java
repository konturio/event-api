package io.kontur.eventapi.firms.episodecomposition;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FirmsFeedCompositionJob extends FeedCompositionJob {

    @Value("${scheduler.feedComposition.firmsAlias}")
    private String[] alias;
    private static AtomicInteger feedCompositionQueueSize;

    public FirmsFeedCompositionJob(KonturEventsDao eventsDao, FeedDao feedDao, NormalizedObservationsDao observationsDao,
                                   List<EpisodeCombinator> episodeCombinators, MeterRegistry meterRegistry) {
        super(eventsDao, feedDao, observationsDao, episodeCombinators, meterRegistry);
        feedCompositionQueueSize = meterRegistry.gauge("feedCompositionJob.queueSize", new AtomicInteger(0));
    }

    @Override
    public void execute() {
        feedCompositionQueueSize.set(eventsDao.getFeedCompositionQueueSize());
        List<Feed> feeds = feedDao.getFeedsByAliases(Arrays.asList(alias));
        feeds.forEach(this::updateFeed);
    }

    @Override
    public String getName() {
        return "firmsFeedComposition";
    }
}
