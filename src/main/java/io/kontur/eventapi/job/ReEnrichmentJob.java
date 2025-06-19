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
        int count = 0;
        for (Feed feed : feedDao.getFeeds()) {
            if (!feed.getEnrichment().isEmpty()) {
                count += reEnrichFeed(feed);
            }
        }
        updateObservationsMetric(count);
    }

    private int reEnrichFeed(Feed feed) {
        List<FeedData> events = feedDao.getEnrichmentSkippedEventsForFeed(feed.getFeedId());
        if (!CollectionUtils.isEmpty(events)) {
            events.forEach(event -> {
                try {
                    longEventEnrichmentTask.enrichEvent(event, feed).get();
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            });
            return events.size();
        }
        return 0;
    }

    @Override
    public String getName() {
        return "reEnrichmentJob";
    }
}
