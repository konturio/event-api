package io.kontur.eventapi.job;

import io.kontur.eventapi.enrichment.EventEnrichmentTask;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static io.kontur.eventapi.enrichment.InsightsApiRequestBuilder.buildParams;

@Component
public class EnrichmentJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(EnrichmentJob.class);
    protected final FeedDao feedDao;
    private final EventEnrichmentTask eventEnrichmentTask;

    public EnrichmentJob(MeterRegistry meterRegistry, FeedDao feedDao, EventEnrichmentTask eventEnrichmentTask) {
        super(meterRegistry);
        this.feedDao = feedDao;
        this.eventEnrichmentTask = eventEnrichmentTask;
    }

    @Override
    public void execute() throws Exception {
        feedDao.getFeeds().stream()
                .filter(feed -> !feed.getEnrichment().isEmpty())
                .forEach(this::enrichFeed);
    }

    @Override
    public String getName() {
        return "enrichmentJob";
    }

    protected void enrichFeed(Feed feed) {
        List<FeedData> events = feedDao.getNotEnrichedEventsForFeed(feed.getFeedId());
        LOG.info(String.format("%s feed. %s events to enrich", feed.getAlias(), events.size()));

        List<String> feedEnrichment = feed.getEnrichment();
        String feedParamsString = buildParams(feedEnrichment);

        var eventEnrichmentTasks = events.stream()
                .map(event -> eventEnrichmentTask.enrichEvent(event, feedEnrichment, feedParamsString))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(eventEnrichmentTasks).join();
    }
}
