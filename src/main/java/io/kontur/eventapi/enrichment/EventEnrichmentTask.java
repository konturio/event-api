package io.kontur.eventapi.enrichment;

import io.kontur.eventapi.client.KonturAppsClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.dto.InsightsApiRequest;
import io.kontur.eventapi.enrichment.dto.InsightsApiResponse;
import io.kontur.eventapi.enrichment.postprocessor.EnrichmentPostProcessor;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.wololo.geojson.FeatureCollection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.kontur.eventapi.enrichment.InsightsApiResponseHandler.processResponse;
import static java.lang.String.format;
import static org.apache.commons.lang3.RegExUtils.replaceAll;

public class EventEnrichmentTask {

    private final Logger LOG = LoggerFactory.getLogger(EventEnrichmentTask.class);
    private final KonturAppsClient konturAppsClient;
    private final FeedDao feedDao;
    private final List<EnrichmentPostProcessor> postProcessors;
    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;

    public EventEnrichmentTask(KonturAppsClient konturAppsClient, FeedDao feedDao,
                               List<EnrichmentPostProcessor> postProcessors, Counter enrichmentSuccessCounter,
                               Counter enrichmentFailCounter) {
        this.konturAppsClient = konturAppsClient;
        this.feedDao = feedDao;
        this.postProcessors = postProcessors;
        this.enrichmentSuccess = enrichmentSuccessCounter;
        this.enrichmentFail = enrichmentFailCounter;
    }


    @Async("enrichmentExecutor")
    @Timed(value = "enrichment.event.timer")
    @Counted(value = "enrichment.event.counter")
    public CompletableFuture<FeedData> enrichEvent(FeedData event, String enrichmentRequest, List<String> enrichmentFields) {
        try {
            processEvent(event, enrichmentRequest, enrichmentFields);
            processEpisodes(event, enrichmentRequest, enrichmentFields);
            applyPostProcessors(event);
            markEventStatus(event);
            feedDao.addAnalytics(event);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        updateMetrics(event);
        return CompletableFuture.completedFuture(event);
    }

    private void processEvent(FeedData event, String enrichmentRequest, List<String> enrichmentFields) throws Exception {
        if (needsEnrichment(event.getEventDetails(), event.getGeometries())) {
            event.setEventDetails(fetchAnalytics(event.getGeometries(), enrichmentRequest, enrichmentFields));
        }
    }

    private void processEpisodes(FeedData event, String enrichmentRequest, List<String> enrichmentFields) throws Exception {
        for (FeedEpisode episode : event.getEpisodes()) {
            if (needsEnrichment(episode.getEpisodeDetails(), episode.getGeometries())) {
                episode.setEpisodeDetails(fetchAnalytics(episode.getGeometries(), enrichmentRequest, enrichmentFields));
            }
        }
    }

    private void applyPostProcessors(FeedData event) {
        postProcessors
                .stream()
                .filter(postProcessor -> postProcessor.isApplicable(event))
                .forEach(postProcessor -> postProcessor.process(event));
    }

    private void markEventStatus(FeedData event) {
        event.setEnrichmentAttempts(event.getEnrichmentAttempts() == null ? 1L : event.getEnrichmentAttempts() + 1L);
        boolean enriched = enriched(event);
        boolean reachedMaxEnrichmentAttempts = event.getEnrichmentAttempts() > 1;
        event.setEnriched(reachedMaxEnrichmentAttempts || enriched);
        event.setEnrichmentSkipped(reachedMaxEnrichmentAttempts && !enriched);
    }

    private void updateMetrics(FeedData event) {
        if (event.getEnriched() && !event.getEnrichmentSkipped()) {
            enrichmentSuccess.increment();
            return;
        }
        enrichmentFail.increment();
        LOG.warn("Event was not enriched: feed_id = '{}', event_id = '{}', version = '{}'",
                event.getFeedId(), event.getEventId(), event.getVersion());
    }

    private Map<String, Object> fetchAnalytics(FeatureCollection geometry, String enrichmentRequest, List<String> enrichmentFields) throws Exception {
        String query = format(enrichmentRequest, replaceAll(geometry.toString(), "\"", "\\\\\\\""));
        InsightsApiRequest request = new InsightsApiRequest(query);
        try {
            InsightsApiResponse response = konturAppsClient.graphql(request);
            return processResponse(response, enrichmentFields);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return null;
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
