package io.kontur.eventapi.job;

import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.EventEnrichmentTask;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static java.lang.String.format;

@Component
public class ReEnrichmentJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReEnrichmentJob.class);
    private final EventEnrichmentTask longEventEnrichmentTask;
    private final FeedDao feedDao;

    public ReEnrichmentJob(MeterRegistry meterRegistry, FeedDao feedDao, EventEnrichmentTask longEventEnrichmentTask) {
        super(meterRegistry);
        this.feedDao = feedDao;
        this.longEventEnrichmentTask = longEventEnrichmentTask;
    }

    @Override
    public void execute() throws Exception {
        feedDao.getFeeds().stream()
                .filter(feed -> !feed.getEnrichment().isEmpty())
                .forEach(this::reEnrichFeed);
    }

    private void reEnrichFeed(Feed feed) {
        List<FeedData> events = feedDao.getEnrichmentSkippedEventsForFeed(feed.getFeedId());
        if (!CollectionUtils.isEmpty(events)) {
            LOG.info(format("%s feed. %s events to re-enrich", feed.getAlias(), events.size()));
;
            events.forEach(event -> {
                try {
                    longEventEnrichmentTask.enrichEvent(event, feed).get();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public String getName() {
        return "reEnrichmentJob";
    }
}
