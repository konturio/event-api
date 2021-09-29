package io.kontur.eventapi.enrichment;

import io.kontur.eventapi.client.KonturAppsClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.postprocessor.EnrichmentPostProcessor;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.kontur.eventapi.enrichment.InsightsApiRequestBuilder.buildRequest;
import static io.kontur.eventapi.enrichment.InsightsApiResponseHandler.processResponse;

@Component
public class EventEnrichmentTask {

    private final Logger LOG = LoggerFactory.getLogger(EventEnrichmentTask.class);
    private final KonturAppsClient konturAppsClient;
    private final FeedDao feedDao;
    private final List<EnrichmentPostProcessor> postProcessors;

    public EventEnrichmentTask(KonturAppsClient konturAppsClient, FeedDao feedDao,
                               List<EnrichmentPostProcessor> postProcessors) {
        this.konturAppsClient = konturAppsClient;
        this.feedDao = feedDao;
        this.postProcessors = postProcessors;
    }


    @Async("enrichmentExecutor")
    public CompletableFuture<FeedData> enrichEvent(FeedData event, List<String> feedEnrichment, String feedParamsString) {
        if (needsEnrichment(event.getEventDetails(), event.getGeometries())) {
            event.setEventDetails(fetchAnalytics(event.getGeometries(), feedEnrichment, feedParamsString));
        }
        for (FeedEpisode episode : event.getEpisodes()) {
            if (needsEnrichment(episode.getEpisodeDetails(), episode.getGeometries())) {
                episode.setEpisodeDetails(fetchAnalytics(episode.getGeometries(), feedEnrichment, feedParamsString));
            }
        }
        postProcessors
                .stream()
                .filter(postProcessor -> postProcessor.isApplicable(event))
                .forEach(postProcessor -> postProcessor.process(event));
        event.setEnriched(enriched(event));
        if (!event.getEnriched()) {
            LOG.warn("Event was not enriched: " + event.getEventId());
        }
        feedDao.addAnalytics(event);
        return CompletableFuture.completedFuture(event);
    }

    private Map<String, Object> fetchAnalytics(FeatureCollection geometry, List<String> feedEnrichment, String feedParamsString) {
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
