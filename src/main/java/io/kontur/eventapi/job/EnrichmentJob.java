package io.kontur.eventapi.job;

import io.kontur.eventapi.enrichment.EventEnrichmentTask;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        try {
            List<CompletableFuture<FeedData>> eventEnrichmentTasks = new ArrayList<>();
            for (Feed feed : getFeeds()) {
                List<FeedData> events = feedDao.getNotEnrichedEventsForFeed(feed.getFeedId());
                if (!CollectionUtils.isEmpty(events)) {
                    eventEnrichmentTasks.addAll(events.stream()
                            .map(event -> eventEnrichmentTask.enrichEvent(event, feed))
                            .toList());
                }
            }
            CompletableFuture.allOf(eventEnrichmentTasks.toArray(CompletableFuture[]::new)).join();
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private List<Feed> getFeeds() {
        return feedDao.getFeeds()
                .stream()
                .filter(feed -> !feed.getEnrichment().isEmpty())
                .toList();
    }

    @Override
    public String getName() {
        return "enrichmentJob";
    }
}
