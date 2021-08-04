package io.kontur.eventapi.job;

import io.kontur.eventapi.enrichment.InsightsApiRequest;
import io.kontur.eventapi.enrichment.InsightsApiResponse;
import io.kontur.eventapi.client.KonturAppsClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;

import java.util.*;

import static io.kontur.eventapi.enrichment.InsightsApiRequestBuilder.buildParams;
import static io.kontur.eventapi.enrichment.InsightsApiRequestBuilder.buildRequest;
import static io.kontur.eventapi.enrichment.InsightsApiResponseHandler.processResponse;

@Component
public class EnrichmentJob extends AbstractJob {

    private final Logger LOG = LoggerFactory.getLogger(EnrichmentJob.class);
    protected final FeedDao feedDao;
    private final KonturAppsClient konturAppsClient;

    public EnrichmentJob(MeterRegistry meterRegistry, FeedDao feedDao, KonturAppsClient konturAppsClient) {
        super(meterRegistry);
        this.feedDao = feedDao;
        this.konturAppsClient = konturAppsClient;
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
        List<String> feedEnrichment = feed.getEnrichment();
        String feedParamsString = buildParams(feedEnrichment);

        List<FeedData> events = feedDao.getNotEnrichedEventsForFeed(feed.getFeedId());
        LOG.info(String.format("%s feed. %s events to enrich", feed.getAlias(), events.size()));
        events.forEach(event -> enrichEvent(event, feedEnrichment, feedParamsString));
    }

    private void enrichEvent(FeedData event, List<String> feedEnrichment, String feedParamsString) {
        if (needsEnrichment(event.getEventDetails(), event.getGeometries())) {
            event.setEventDetails(getAnalytics(event.getGeometries(), feedEnrichment, feedParamsString));
        }
        for (FeedEpisode episode : event.getEpisodes()) {
            if (needsEnrichment(episode.getEpisodeDetails(), episode.getGeometries())) {
                episode.setEpisodeDetails(getAnalytics(episode.getGeometries(), feedEnrichment, feedParamsString));
            }
        }
        event.setEnriched(enriched(event));
        feedDao.addAnalytics(event);
    }

    private Map<String, Object> getAnalytics(FeatureCollection geometry, List<String> feedEnrichment, String feedParamsString) {
        InsightsApiRequest request = buildRequest(geometry, feedParamsString);
        try {
            InsightsApiResponse response = konturAppsClient.graphql(request);
            return processResponse(response, feedEnrichment);
        } catch (Exception e) {
            LOG.error(e.getMessage() + "\n" + request.getQuery());
            return null;
        }
    }

    private boolean needsEnrichment(Map<String, Object> details, FeatureCollection geometries) {
        return (details == null || details.isEmpty()) &&
                geometries != null && geometries.getFeatures() != null && geometries.getFeatures().length > 0;
    }

    private boolean enriched(FeedData event) {
        return !needsEnrichment(event.getEventDetails(), event.getGeometries()) &&
                event.getEpisodes().stream().noneMatch(episode ->
                        needsEnrichment(episode.getEpisodeDetails(), episode.getGeometries()));
    }
}
