package io.kontur.eventapi.firms.event;

import io.kontur.eventapi.client.KonturApiClient;
import io.kontur.eventapi.dao.KonturEventsDao;
import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.firms.client.FirmsClient;
import io.kontur.eventapi.firms.jobs.FirmsImportJob;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import io.kontur.eventapi.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.wololo.geojson.FeatureCollection;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.TestUtil.readFile;
import static java.time.OffsetDateTime.parse;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class FirmsEventAndEpisodeCombinationsJobIT extends AbstractCleanableIntegrationTest {
    private final FirmsImportJob firmsImportJob;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final FeedMapper feedMapper;
    private final KonturEventsDao konturEventsDao;
    private final NormalizedObservationsDao observationsDao;

    @MockBean
    private FirmsClient firmsClient;
    @MockBean
    private KonturApiClient konturApiClient;

    @Autowired
    public FirmsEventAndEpisodeCombinationsJobIT(FirmsImportJob firmsImportJob, NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, FeedMapper feedMapper, KonturEventsDao konturEventsDao, NormalizedObservationsDao observationsDao, JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.firmsImportJob = firmsImportJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.feedMapper = feedMapper;
        this.konturEventsDao = konturEventsDao;
        this.observationsDao = observationsDao;
    }

    @Test
    public void testFirmsEventAndEpisodesRollout() throws IOException {
        //GIVEN 3 observation within 1 km, 2 of them have same date. And 1 other observation
        when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6.csv"));
        when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));
        configureKonturApiClient();

        //WHEN run event job
        firmsImportJob.run();
        normalizationJob.run();
        eventCombinationJob.run();

        //THEN
        var firmsFeed = feedMapper.getFeeds().stream().filter(feed -> feed.getAlias().equals("firms")).findFirst().orElseThrow();

        List<KonturEvent> eventsForRolloutEpisodes = readEvents(konturEventsDao.getEventsForRolloutEpisodes(firmsFeed.getFeedId()));

        assertEquals(2, eventsForRolloutEpisodes.size());//2 group of observations which are far from each other (> 1km)
        assertEquals(1, eventsForRolloutEpisodes.get(0).getObservationIds().size());
        assertEquals(3, eventsForRolloutEpisodes.get(1).getObservationIds().size());

        //WHEN run feed job
        feedCompositionJob.run();
        List<FeedData> feedData = searchFeedData();

        //THEN
        assertEquals(2, feedData.size());

        assertEquals(1, feedData.get(0).getObservations().size());
        assertEquals(1, feedData.get(0).getEpisodes().size());

        assertEquals(3, feedData.get(1).getObservations().size());//3 observations within 1 km
        assertEquals(2, feedData.get(1).getEpisodes().size());//2 observations have same date

        assertEquals("Thermal anomaly in Brazil, North Region, Para. Burnt area 0.871 km\u00B2, burning 27 hours.", feedData.get(1).getEpisodes().get(1).getName());
        assertEquals(3, feedData.get(1).getEpisodes().get(1).getObservations().size());


        //WHEN new data available for modis - 3 observations within 1 km to 2 existing observation
        //and 1 other observation
        when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6-update.csv"));
        when(firmsClient.getNoaa20VirsData()).thenReturn(readCsv("firms.suomi-npp-viirs-c2.csv"));
        when(firmsClient.getSuomiNppVirsData()).thenReturn(readCsv("firms.noaa-20-viirs-c2.csv"));

        firmsImportJob.run();
        normalizationJob.run();
        eventCombinationJob.run();

        //THEN
        List<KonturEvent> eventsForRolloutEpisodesUpdated = readEvents(konturEventsDao.getEventsForRolloutEpisodes(firmsFeed.getFeedId()));

        assertEquals(3, eventsForRolloutEpisodesUpdated.size());//3 group of observations with are far from each other (> 1km)
        assertEquals(1, eventsForRolloutEpisodesUpdated.get(0).getObservationIds().size());
        assertEquals(2, eventsForRolloutEpisodesUpdated.get(1).getObservationIds().size());
        assertEquals(5, eventsForRolloutEpisodesUpdated.get(2).getObservationIds().size());

        //WHEN run feed job again
        feedCompositionJob.run();

        //THEN
        List<FeedData> firmsUpdated = searchFeedData();

        assertEquals(3, firmsUpdated.size());

        assertEquals(1, firmsUpdated.get(0).getObservations().size());
        assertEquals(1, firmsUpdated.get(0).getEpisodes().size());
        assertEquals(1, firmsUpdated.get(0).getVersion());

        assertEquals(2, firmsUpdated.get(1).getObservations().size());
        assertEquals(2, firmsUpdated.get(1).getEpisodes().size());
        assertEquals(2, firmsUpdated.get(1).getVersion());

        FeedData someFedData = firmsUpdated.get(2);
        assertEquals("Thermal anomaly in an unknown area. Burnt area 2.613 km\u00B2, burning 35 hours.", someFedData.getName());
        assertEquals(5, someFedData.getObservations().size());
        assertEquals(parse("2020-11-02T11:50Z"),someFedData.getStartedAt());
        assertEquals(parse("2020-11-03T22:50Z"),someFedData.getEndedAt());
        assertEquals(2, someFedData.getVersion());

        List<FeedEpisode> episodes = someFedData.getEpisodes();
        assertEquals(4, episodes.size());

        episodes.sort(Comparator.comparing(FeedEpisode::getSourceUpdatedAt));

        assertEquals("Thermal anomaly in Brazil, North Region, Para. Burnt area 0.871 km\u00B2",
                episodes.get(0).getName());
        assertEquals(2, episodes.get(0).getObservations().size());
        assertEquals(parse("2020-11-02T11:50Z"), episodes.get(0).getSourceUpdatedAt());
        assertEquals(parse("2020-11-02T11:50Z"), episodes.get(0).getStartedAt());
        assertEquals(parse("2020-11-02T12:50Z"), episodes.get(0).getEndedAt());
        assertEquals(Severity.MINOR, episodes.get(0).getSeverity());

        assertEquals("Thermal anomaly in an unknown area. Burnt area 1.742 km\u00B2", episodes.get(1).getName());
        assertEquals(3, episodes.get(1).getObservations().size());
        assertEquals(parse("2020-11-02T12:50Z"), episodes.get(1).getSourceUpdatedAt());
        assertEquals(parse("2020-11-02T12:50Z"), episodes.get(1).getStartedAt());
        assertEquals(parse("2020-11-02T14:50Z"), episodes.get(1).getEndedAt());
        assertEquals(Severity.MINOR, episodes.get(1).getSeverity());

        assertEquals("Thermal anomaly in Brazil, North Region, Para. Burnt area 1.742 km\u00B2", episodes.get(2).getName());
        assertEquals(4, episodes.get(2).getObservations().size());
        assertEquals(parse("2020-11-02T14:50Z"), episodes.get(2).getSourceUpdatedAt());
        assertEquals(parse("2020-11-02T14:50Z"), episodes.get(2).getStartedAt());
        assertEquals(parse("2020-11-02T22:50Z"), episodes.get(2).getEndedAt());
        assertEquals(Severity.MINOR, episodes.get(2).getSeverity());

        assertEquals("Thermal anomaly in an unknown area. Burnt area 2.613 km\u00B2, burning 35 hours.", episodes.get(3).getName());
        assertEquals(5, episodes.get(3).getObservations().size());
        assertEquals(parse("2020-11-02T22:50Z"), episodes.get(3).getSourceUpdatedAt());
        assertEquals(parse("2020-11-02T22:50Z"), episodes.get(3).getStartedAt());
        assertEquals(parse("2020-11-03T22:50Z"), episodes.get(3).getEndedAt());
        assertEquals(Severity.MINOR, episodes.get(3).getSeverity());

        List<KonturEvent> newEventsForRolloutEpisodes = readEvents(konturEventsDao.getEventsForRolloutEpisodes(firmsFeed.getFeedId()));
        assertTrue(newEventsForRolloutEpisodes.isEmpty());

        //WHEN new data available for modis - 1 observations within 1 km to existing observations
        when(firmsClient.getModisData()).thenReturn(readCsv("firms.modis-c6-update-2.csv"));

        firmsImportJob.run();
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        //THEN area is calculated from all hexaons that were burning
        List<FeedData> firmsUpdated2 = searchFeedData();

        assertEquals(3, firmsUpdated2.size());
        Optional<FeedData> updatedEvent = firmsUpdated2.stream().filter(event -> event.getVersion() == 3).findFirst();
        assertTrue(updatedEvent.isPresent());
        assertEquals("Thermal anomaly in an unknown area. Burnt area 3.484 km\u00B2, burning 53 hours.", updatedEvent.get().getEpisodes().get(4).getName());
    }

    private void configureKonturApiClient() {
        when(konturApiClient.adminBoundaries("POINT (145.96183 -34.74616)", 10))
                .then((i) -> JsonUtil.readJson("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"name\":\"Pará\",\"tags\":{\"ref\":\"PA\",\"name\":\"Para\",\"admin_level\":\"4\"},\"osm_id\":185579,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"4\"},\"id\":\"185579\"},{\"type\":\"Feature\",\"properties\":{\"name\":\"Região Geográfica Intermediária de Santarém\",\"tags\":{\"name\":\"Região Geográfica Intermediária de Santarém\",\"boundary\":\"administrative\",\"wikidata\":\"Q65167712\",\"wikipedia\":\"pt:Região Geográfica Intermediária de Santarém\",\"admin_level\":\"5\",\"IBGE:GEOCODIGO\":\"1505\"},\"osm_id\":4826842,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"5\"},\"id\":\"4826842\"},{\"type\":\"Feature\",\"properties\":{\"name\":\"Regional Coastline\",\"tags\":{\"name\":\"Regional Coastline\",\"boundary\":\"administrative\",\"admin_level\":\"Regional\"},\"osm_id\":9969273,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"Regional\"},\"id\":\"9969273\"},{\"type\":\"Feature\",\"properties\":{\"name\":\"Região Norte\",\"tags\":{\"name\":\"Região Norte\",\"name:en\":\"North Region\",\"admin_level\":\"3\",\"is_in:country\":\"Brazil\"},\"osm_id\":3360778,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"3\"},\"id\":\"3360778\"},{\"type\":\"Feature\",\"properties\":{\"name\":\"Brasil\",\"tags\":{\"name\":\"Brasil\",\"int_name\":\"Brazil\",\"admin_level\":\"2\"},\"osm_id\":59470,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"2\"},\"id\":\"59470\"},{\"type\":\"Feature\",\"properties\":{\"name\":\"Região Geográfica Imediata de Oriximiná\",\"tags\":{\"name\":\"Região Geográfica Imediata de Oriximiná\",\"boundary\":\"administrative\",\"wikidata\":\"Q2596742\",\"wikipedia\":\"pt:Microrregião de Tucuruí\",\"admin_level\":\"7\",\"IBGE:GEOCODIGO\":\"150616\"},\"osm_id\":12115100,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"7\"},\"id\":\"12115100\",\"links\":[{\"href\":\"https://test-api02.konturlabs.com:443/layers/collections/bounds/items/12115100\",\"rel\":\"self\",\"type\":\"application/geo+json\"},{\"href\":\"https://test-api02.konturlabs.com:443/layers/collections/bounds\",\"rel\":\"collection\",\"type\":\"application/geo+json\",\"title\":\"Admin Boundaries\"}]},{\"type\":\"Feature\",\"properties\":{\"name\":\"Oriximiná\",\"tags\":{\"name\":\"Oriximiná\",\"boundary\":\"administrative\",\"wikidata\":\"Q2011919\",\"wikipedia\":\"pt:Oriximiná\",\"population\":\"57765\",\"admin_level\":\"8\",\"IBGE:GEOCODIGO\":\"1505304\"},\"osm_id\":185652,\"boundary\":\"administrative\",\"osm_type\":\"relation\",\"admin_level\":\"8\"},\"id\":\"185652\",\"links\":[{\"href\":\"https://test-api02.konturlabs.com:443/layers/collections/bounds/items/185652\",\"rel\":\"self\",\"type\":\"application/geo+json\"},{\"href\":\"https://test-api02.konturlabs.com:443/layers/collections/bounds\",\"rel\":\"collection\",\"type\":\"application/geo+json\",\"title\":\"Admin Boundaries\"}]}],\"links\":[{\"href\":\"https://test-api02.konturlabs.com:443/layers/collections/bounds/items?limit=10&offset=0\",\"rel\":\"self\",\"type\":\"application/geo+json\",\"title\":\"Admin Boundaries\"}],\"timeStamp\":\"2021-02-19T06:22:13Z\",\"numberMatched\":7,\"numberReturned\":7}",
                        FeatureCollection.class));

    }

    private List<KonturEvent> readEvents(Set<UUID> eventsForRolloutEpisodes1) {
        return eventsForRolloutEpisodes1
                .stream()
                .map(e -> new KonturEvent(e).setObservationIds(observationsDao.getObservationsByEventId(e).stream()
                        .map(NormalizedObservation::getObservationId).collect(toList())))
                .sorted(Comparator.comparing(e -> e.getObservationIds().size()))
                .collect(toList());
    }

    private List<FeedData> searchFeedData() {
        List<FeedData> firms = feedMapper.searchForEvents(
                "firms",
                List.of(EventType.THERMAL_ANOMALY),
                null,
                null,
                OffsetDateTime.parse("2020-11-02T11:00Z"),
                100,
                List.of(),
                SortOrder.ASC,
                null,
                null,
                null,
                null
        );
        firms.sort(Comparator.comparing(f -> f.getObservations().size()));
        return firms;
    }

    private String readCsv(String fileName) throws IOException {
        return readFile(this,fileName);
    }

}