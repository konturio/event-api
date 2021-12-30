package io.kontur.eventapi.enrichment;

import io.kontur.eventapi.client.KonturAppsClient;
import io.kontur.eventapi.client.LongKonturAppsClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.enrichment.postprocessor.EnrichmentPostProcessor;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class EnrichmentConfig {
    public static final String POPULATION = "population";
    public static final String OSM_GAPS_PERCENTAGE = "osmGapsPercentage";
    public static final String INDUSTRIAL_AREA_KM2 = "industrialAreaKm2";
    public static final String FOREST_AREA_KM2 = "forestAreaKm2";
    public static final String VOLCANOES_COUNT = "volcanoesCount";
    public static final String HOTSPOT_DAYS_PER_YEAR_MAX = "hotspotDaysPerYearMax";
    public static final String POPULATED_AREA_KM2 = "populatedAreaKm2";

    @Bean
    public EventEnrichmentTask eventEnrichmentTask(@Qualifier("konturAppsClient") KonturAppsClient konturAppsClient,
                                                   FeedDao feedDao, List<EnrichmentPostProcessor> postProcessors,
                                                   Counter enrichmentSuccessCounter, Counter enrichmentFailCounter) {
        return new EventEnrichmentTask(konturAppsClient, feedDao, postProcessors,
                enrichmentSuccessCounter, enrichmentFailCounter);
    }

    @Bean
    public EventEnrichmentTask longEventEnrichmentTask(@Qualifier("longKonturAppsClient") LongKonturAppsClient longKonturAppsClient,
                                                       FeedDao feedDao, List<EnrichmentPostProcessor> postProcessors,
                                                       Counter enrichmentSuccessCounter, Counter enrichmentFailCounter) {
        return new EventEnrichmentTask(longKonturAppsClient, feedDao, postProcessors,
                enrichmentSuccessCounter, enrichmentFailCounter);
    }
}
