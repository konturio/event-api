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
import java.util.concurrent.atomic.AtomicInteger;

import static io.kontur.eventapi.enrichment.InsightsApiRequestBuilder.buildParams;

@Component
public class EnrichmentJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(EnrichmentJob.class);
    protected final FeedDao feedDao;
    private final EventEnrichmentTask eventEnrichmentTask;
    private final AtomicInteger enrichmentSuccess;
    private final AtomicInteger enrichmentFail;
    private final AtomicInteger enrichmentQueueSize;

    public EnrichmentJob(MeterRegistry meterRegistry, FeedDao feedDao, EventEnrichmentTask eventEnrichmentTask,
                         AtomicInteger enrichmentSuccessGauge, AtomicInteger enrichmentFailGauge,
                         AtomicInteger enrichmentQueueSizeGauge) {
        super(meterRegistry);
        this.feedDao = feedDao;
        this.eventEnrichmentTask = eventEnrichmentTask;
        this.enrichmentSuccess = enrichmentSuccessGauge;
        this.enrichmentFail = enrichmentFailGauge;
        this.enrichmentQueueSize = enrichmentQueueSizeGauge;
    }

    @Override
    public void execute() throws Exception {
        feedDao.getFeeds().stream()
                .filter(feed -> !feed.getEnrichment().isEmpty())
                .forEach(this::enrichFeed);
        enrichmentSuccess.set(0);
        enrichmentFail.set(0);
        enrichmentQueueSize.set(feedDao.getNotEnrichedEventsCount());
    }

    @Override
    public String getName() {
        return "enrichmentJob";
    }

    protected void enrichFeed(Feed feed) {
        try {
            List<FeedData> events = feedDao.getNotEnrichedEventsForFeed(feed.getFeedId());
            LOG.info(String.format("%s feed. %s events to enrich", feed.getAlias(), events.size()));

            List<String> feedEnrichment = feed.getEnrichment();
            String feedParamsString = buildParams(feedEnrichment);

            var eventEnrichmentTasks = events.stream()
                    .map(event -> eventEnrichmentTask.enrichEvent(event, feedEnrichment, feedParamsString))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(eventEnrichmentTasks).join();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}
