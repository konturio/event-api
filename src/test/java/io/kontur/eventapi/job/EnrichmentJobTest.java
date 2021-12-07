package io.kontur.eventapi.job;

import io.kontur.eventapi.client.KonturAppsClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.postprocessor.EnrichmentPostProcessor;
import io.kontur.eventapi.enrichment.EventEnrichmentTask;
import io.kontur.eventapi.enrichment.dto.InsightsApiRequest;
import io.kontur.eventapi.enrichment.dto.InsightsApiResponse;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.kontur.eventapi.enrichment.EnrichmentConfig.*;
import static org.mockito.Mockito.*;

public class EnrichmentJobTest {

    private final FeedDao feedDao = mock(FeedDao.class);
    private final KonturAppsClient konturAppsClient = mock(KonturAppsClient.class);
    private final List<EnrichmentPostProcessor> postProcessors = new ArrayList<>();
    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;

    public EnrichmentJobTest() {
        MeterRegistry registry = new SimpleMeterRegistry();
        this.enrichmentSuccess = registry.counter("enrichment.success.counter");
        this.enrichmentFail = registry.counter("enrichment.fail.counter");
    }

    private static final Feed feedWithEnrichment;
    static {
        feedWithEnrichment = new Feed();
        feedWithEnrichment.setFeedId(UUID.randomUUID());
        feedWithEnrichment.setAlias("feedWithEnrichment");
        feedWithEnrichment.setEnrichment(List.of(POPULATION, OSM_GAPS_PERCENTAGE));
        feedWithEnrichment.setEnrichmentRequest("{polygonStatistic (polygonStatisticRequest: {polygon: \"%s\"}){analytics {population {population}}}}");
    }

    private static final Feed feedWithoutEnrichment;
    static {
        feedWithoutEnrichment = new Feed();
        feedWithoutEnrichment.setFeedId(UUID.randomUUID());
        feedWithoutEnrichment.setAlias("feedWithoutEnrichment");
        feedWithoutEnrichment.setEnrichment(Collections.emptyList());
    }

    @AfterEach
    public void resetMocks() {
        reset(feedDao);
        reset(konturAppsClient);
    }

    @Test
    public void testExecute() {
        FeedData notEnrichedFeedData = createFeedData(feedWithEnrichment, false);

        when(feedDao.getFeeds()).thenReturn(List.of(feedWithEnrichment, feedWithoutEnrichment));
        when(feedDao.getNotEnrichedEventsForFeed(feedWithEnrichment.getFeedId())).thenReturn(List.of(notEnrichedFeedData));
        doNothing().when(feedDao).addAnalytics(any());
        when(konturAppsClient.graphql(isA(InsightsApiRequest.class))).thenReturn(createResponse());
        EventEnrichmentTask enrichmentTask = new EventEnrichmentTask(konturAppsClient, feedDao, postProcessors,
                enrichmentSuccess, enrichmentFail);
        EnrichmentJob enrichmentJob = new EnrichmentJob(new SimpleMeterRegistry(), feedDao, enrichmentTask);

        enrichmentJob.run();

        verify(konturAppsClient, times(2)).graphql(isA(InsightsApiRequest.class));
        verify(feedDao, times(1)).getNotEnrichedEventsForFeed(feedWithEnrichment.getFeedId());
        verify(feedDao, times(0)).getNotEnrichedEventsForFeed(feedWithoutEnrichment.getFeedId());
        verify(feedDao, times(1)).addAnalytics(any());
    }

    @Test
    public void testExecuteWhenError() {
        FeedData notEnrichedFeedData = createFeedData(feedWithEnrichment, false);

        when(feedDao.getFeeds()).thenReturn(List.of(feedWithEnrichment, feedWithoutEnrichment));
        when(feedDao.getNotEnrichedEventsForFeed(feedWithEnrichment.getFeedId())).thenReturn(List.of(notEnrichedFeedData));
        doNothing().when(feedDao).addAnalytics(any());
        when(konturAppsClient.graphql(isA(InsightsApiRequest.class))).thenReturn(createErrorResponse());
        EventEnrichmentTask enrichmentTask = new EventEnrichmentTask(konturAppsClient, feedDao, postProcessors,
                enrichmentSuccess, enrichmentFail);
        EnrichmentJob enrichmentJob = new EnrichmentJob(new SimpleMeterRegistry(), feedDao, enrichmentTask);

        enrichmentJob.run();

        verify(konturAppsClient, times(2)).graphql(isA(InsightsApiRequest.class));
        verify(feedDao, times(1)).getNotEnrichedEventsForFeed(feedWithEnrichment.getFeedId());
        verify(feedDao, times(0)).getNotEnrichedEventsForFeed(feedWithoutEnrichment.getFeedId());
        verify(feedDao, times(1)).addAnalytics(any());
    }

    private FeedData createFeedData(Feed feed, boolean enriched) {
        FeedData feedData = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);
        feedData.setEnriched(enriched);
        feedData.addEpisode(createFeedEpisode());
        feedData.setGeometries(createGeometries());
        feedData.setEnrichmentAttempts(1L);
        return feedData;
    }

    private FeedEpisode createFeedEpisode() {
        FeedEpisode feedEpisode = new FeedEpisode();
        feedEpisode.setGeometries(createGeometries());
        return feedEpisode;
    }

    private FeatureCollection createGeometries() {
        Point point = new Point(new double[] {10, 10});
        Feature feature = new Feature(point, Collections.emptyMap());
        return new FeatureCollection(new Feature[] {feature});
    }

    private InsightsApiResponse createResponse() {
        InsightsApiResponse.Population population = new InsightsApiResponse.Population();
        population.setPopulation(0L);
        List<InsightsApiResponse.AnalyticFunction> functions = new ArrayList<>();
        InsightsApiResponse.Analytics populationStatistic = new InsightsApiResponse.Analytics();
        populationStatistic.setPopulation(population);
        populationStatistic.setFunctions(functions);
        InsightsApiResponse.PolygonStatistic polygonStatistic = new InsightsApiResponse.PolygonStatistic();
        polygonStatistic.setAnalytics(populationStatistic);
        InsightsApiResponse.ResponseData responseData = new InsightsApiResponse.ResponseData();
        responseData.setPolygonStatistic(polygonStatistic);
        InsightsApiResponse response = new InsightsApiResponse();
        response.setData(responseData);
        return response;
    }

    private InsightsApiResponse createErrorResponse() {
        InsightsApiResponse.ResponseError error = new InsightsApiResponse.ResponseError();
        error.setMessage("Internal Server Error");
        InsightsApiResponse response = new InsightsApiResponse();
        response.setErrors(List.of(error));
        return response;
    }
}