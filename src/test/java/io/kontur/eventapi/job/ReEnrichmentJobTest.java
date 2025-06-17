package io.kontur.eventapi.job;

import io.kontur.eventapi.client.LongKonturAppsClient;
import io.kontur.eventapi.enrichment.EventEnrichmentTask;
import io.kontur.eventapi.enrichment.dto.InsightsApiRequest;
import io.kontur.eventapi.enrichment.dto.InsightsApiResponse;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.postprocessor.EnrichmentPostProcessor;
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

import static io.kontur.eventapi.enrichment.EnrichmentConfig.OSM_GAPS_PERCENTAGE;
import static io.kontur.eventapi.enrichment.EnrichmentConfig.POPULATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class ReEnrichmentJobTest {

    private final FeedDao feedDao = mock(FeedDao.class);
    private final LongKonturAppsClient longKonturAppsClient = mock(LongKonturAppsClient.class);
    private final List<EnrichmentPostProcessor> postProcessors = new ArrayList<>();
    private final Counter enrichmentSuccess;
    private final Counter enrichmentFail;

    public ReEnrichmentJobTest() {
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
        feedWithEnrichment.setEnrichmentRequest("query ($polygon: String!) { polygonStatistic(polygonStatisticRequest: {polygon: $polygon}){analytics {population {population}}}}");
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
        reset(longKonturAppsClient);
    }

    @Test
    public void testExecute() {
        FeedData skippedEvent = createFeedData(feedWithEnrichment);

        when(feedDao.getFeeds()).thenReturn(List.of(feedWithEnrichment, feedWithoutEnrichment));
        when(feedDao.getEnrichmentSkippedEventsForFeed(feedWithEnrichment.getFeedId())).thenReturn(List.of(skippedEvent));
        doNothing().when(feedDao).addAnalytics(any(), any());
        when(longKonturAppsClient.graphql(isA(InsightsApiRequest.class))).thenReturn(createResponse());
        EventEnrichmentTask enrichmentTask = new EventEnrichmentTask(longKonturAppsClient, feedDao, postProcessors, enrichmentSuccess, enrichmentFail);
        ReEnrichmentJob reEnrichmentJob = new ReEnrichmentJob(new SimpleMeterRegistry(), feedDao, enrichmentTask);

        reEnrichmentJob.run();

        verify(longKonturAppsClient, times(2)).graphql(isA(InsightsApiRequest.class));
        verify(feedDao, times(1)).getEnrichmentSkippedEventsForFeed(feedWithEnrichment.getFeedId());
        verify(feedDao, times(0)).getEnrichmentSkippedEventsForFeed(feedWithoutEnrichment.getFeedId());
        verify(feedDao, times(1)).addAnalytics(any(), any());
    }

    @Test
    public void testExecuteWhenError() {
        FeedData skippedEvent = createFeedData(feedWithEnrichment);

        when(feedDao.getFeeds()).thenReturn(List.of(feedWithEnrichment, feedWithoutEnrichment));
        when(feedDao.getEnrichmentSkippedEventsForFeed(feedWithEnrichment.getFeedId())).thenReturn(List.of(skippedEvent));
        doNothing().when(feedDao).addAnalytics(any(), any());
        when(longKonturAppsClient.graphql(isA(InsightsApiRequest.class))).thenReturn(createErrorResponse());
        EventEnrichmentTask enrichmentTask = new EventEnrichmentTask(longKonturAppsClient, feedDao, postProcessors, enrichmentSuccess, enrichmentFail);
        ReEnrichmentJob reEnrichmentJob = new ReEnrichmentJob(new SimpleMeterRegistry(), feedDao, enrichmentTask);

        reEnrichmentJob.run();

        verify(longKonturAppsClient, times(2)).graphql(isA(InsightsApiRequest.class));
        verify(feedDao, times(1)).getEnrichmentSkippedEventsForFeed(feedWithEnrichment.getFeedId());
        verify(feedDao, times(0)).getEnrichmentSkippedEventsForFeed(feedWithoutEnrichment.getFeedId());
        verify(feedDao, times(1)).addAnalytics(any(), any());
    }

    private FeedData createFeedData(Feed feed) {
        FeedData feedData = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);
        feedData.setEnriched(true);
        feedData.addEpisode(createFeedEpisode());
        feedData.setGeometries(createGeometries());
        feedData.setEnrichmentAttempts(2L);
        feedData.setEnrichmentSkipped(true);
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