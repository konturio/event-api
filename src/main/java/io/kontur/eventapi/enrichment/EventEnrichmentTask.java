package io.kontur.eventapi.enrichment;

import io.kontur.eventapi.client.KonturAppsClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.dto.InsightsApiRequest;
import io.kontur.eventapi.enrichment.dto.InsightsApiResponse;
import io.kontur.eventapi.enrichment.postprocessor.EnrichmentPostProcessor;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;

import java.util.*;
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
    public CompletableFuture<FeedData> enrichEvent(FeedData event, Feed feed) {
        try {
            processEvent(event, feed);
            processEpisodes(event, feed);
            applyPostProcessors(event, feed);
            markEventStatus(event);
            feedDao.addAnalytics(event, feed.getAlias());
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        updateMetrics(event);
        return CompletableFuture.completedFuture(event);
    }

    private void processEvent(FeedData event, Feed feed) {
        if (needsEnrichment(event.getEventDetails(), event.getGeometries())) {
            fetchAnalytics(event.getGeometries(), feed.getEnrichmentRequest(), feed.getEnrichment())
                    .ifPresent(event::setEventDetails);
        }
    }

    private void processEpisodes(FeedData event, Feed feed) {
        for (FeedEpisode episode : event.getEpisodes()) {
            if (needsEnrichment(episode.getEpisodeDetails(), episode.getGeometries())) {
                fetchAnalytics(episode.getGeometries(), feed.getEnrichmentRequest(), feed.getEnrichment())
                        .ifPresent(episode::setEpisodeDetails);
            }
        }
    }

    private void applyPostProcessors(FeedData event, Feed feed) {
        postProcessors
                .stream()
                .filter(postProcessor -> postProcessor.isApplicable(feed))
                .forEach(postProcessor -> postProcessor.process(event));
    }

    private void markEventStatus(FeedData event) {
        event.setEnrichmentAttempts(event.getEnrichmentAttempts() == null ? 1L : event.getEnrichmentAttempts() + 1L);
        boolean enriched = enriched(event);
        event.setEnriched(true);
        event.setEnrichmentSkipped(!enriched);
    }

    private void updateMetrics(FeedData event) {
        if (event.getEnriched() && !event.getEnrichmentSkipped()) {
            enrichmentSuccess.increment();
            return;
        }
        enrichmentFail.increment();
        LOG.warn("Event was not enriched: feed_id = '{}', event_id = '{}', version = {}",
                event.getFeedId(), event.getEventId(), event.getVersion());
    }

    private Optional<Map<String, Object>> fetchAnalytics(FeatureCollection fc, String enrichmentRequest, List<String> enrichmentFields) {
        try {
            Feature[] features = Arrays.stream(fc.getFeatures())
                    .map(Feature::getGeometry)
                    .filter(Objects::nonNull)
                    .map(geometry -> new Feature(geometry, new HashMap<>()))
                    .toArray(Feature[]::new);
            FeatureCollection geom = new FeatureCollection(features);
            String query = format(enrichmentRequest, replaceAll(geom.toString(), "\"", "\\\\\\\""));
            InsightsApiRequest request = new InsightsApiRequest(query);
            InsightsApiResponse response = konturAppsClient.graphql(request);
            return Optional.of(processResponse(response, enrichmentFields));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean needsEnrichment(Map<String, Object> details, FeatureCollection fc) {
        return (details == null || details.isEmpty()) &&
                fc != null && fc.getFeatures() != null && fc.getFeatures().length > 0 &&
                Arrays.stream(fc.getFeatures()).anyMatch(feature -> feature.getGeometry() != null);
    }

    private boolean enriched(FeedData event) {
        return !needsEnrichment(event.getEventDetails(), event.getGeometries()) &&
                event.getEpisodes().stream().noneMatch(episode ->
                        needsEnrichment(episode.getEpisodeDetails(), episode.getGeometries()));
    }
}
