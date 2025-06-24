package io.kontur.eventapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.resource.dto.TestEventDto;
import io.kontur.eventapi.resource.dto.TestEventListDto;
import io.kontur.eventapi.test.AbstractCleanableIntegrationTest;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.shaded.com.google.common.collect.Iterables;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONWriter;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.entity.SortOrder.ASC;
import static io.kontur.eventapi.entity.SortOrder.DESC;
import static io.kontur.eventapi.resource.dto.EpisodeFilterType.ANY;
import io.kontur.eventapi.resource.dto.GeometryFilterType;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventResourceServiceIT extends AbstractCleanableIntegrationTest {

    private final FeedDao feedDao;
    private final EventResourceService eventResourceService;
    private final WKTReader wktReader = new WKTReader();
    private final GeoJSONWriter geoJSONWriter = new GeoJSONWriter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String feedAlias = "test-feed";
    private final UUID eventUUID = UUID.randomUUID();

    @Autowired
    public EventResourceServiceIT(FeedDao feedDao, EventResourceService eventResourceService, JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, feedDao);
        this.feedDao = feedDao;
        this.eventResourceService = eventResourceService;
    }

    @PostConstruct
    public void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void checkFilterByBbox() throws ParseException, IOException {
        //given
        var feed = feedDao.getFeeds().stream()
                .filter(f -> f.getAlias().equals(feedAlias))
                .findFirst()
                .orElseThrow();

        var feedData = new FeedData(eventUUID, feed.getFeedId(), 1L);
        feedData.setUpdatedAt(OffsetDateTime.now());
        feedData.setStartedAt(dateTimeOf(2020, 5, 1));
        feedData.setEndedAt(dateTimeOf(2020, 6, 1));
        FeedEpisode feedEpisode = new FeedEpisode();
        Geometry write = geoJSONWriter.write(wktReader.read("POLYGON ((10 20, 11 21, 12 22, 10 20))"));
        feedEpisode.setGeometries(new FeatureCollection(new Feature[]{new Feature(write, Map.of())}));
        feedData.getEpisodes().add(feedEpisode);
        feedData.setEnriched(true);
        feedDao.insertFeedData(feedData, feedAlias);

        //when-then
        assertEquals(fetchEvent(getBbox(10, 20, 12, 22)).size(), 1);
        assertEquals(fetchEvent(getBbox(50, 60, 52, 62)).size(), 0);
    }

    private List<BigDecimal> getBbox(int i, int i2, int i3, int i4) {
        return List.of(BigDecimal.valueOf(i), BigDecimal.valueOf(i2), BigDecimal.valueOf(i3), BigDecimal.valueOf(i4));
    }

    @Test
    public void checkFilterByDates() throws IOException {
        var feed = feedDao.getFeeds().stream()
                .filter(f -> f.getAlias().equals(feedAlias))
                .findFirst()
                .orElseThrow();

        var feedData = new FeedData(eventUUID, feed.getFeedId(), 1L);
        feedData.setUpdatedAt(OffsetDateTime.now());
        feedData.setStartedAt(dateTimeOf(2020, 5, 1));
        feedData.setEndedAt(dateTimeOf(2020, 6, 1));
        feedData.setEnriched(true);
        feedDao.insertFeedData(feedData, feedAlias);

        var resultOfSearching01 = findEvent(dateTimeOf(2020, 4, 15), dateTimeOf(2020, 5, 15));
        assertTrue(resultOfSearching01.isPresent());

        var resultOfSearching02 = findEvent(dateTimeOf(2020, 5, 15), dateTimeOf(2020, 6, 15));
        assertTrue(resultOfSearching02.isPresent());

        var resultOfSearching03 = findEvent(dateTimeOf(2020, 4, 15), dateTimeOf(2020, 5, 15));
        assertTrue(resultOfSearching03.isPresent());

        var resultOfSearching04 = findEvent(dateTimeOf(2020, 5, 10), dateTimeOf(2020, 5, 15));
        assertTrue(resultOfSearching04.isPresent());

        var resultOfSearching05 = findEvent(dateTimeOf(2020, 7, 10), dateTimeOf(2020, 8, 15));
        assertFalse(resultOfSearching05.isPresent());

        var resultOfSearching06 = findEvent(dateTimeOf(2020, 1, 10), dateTimeOf(2020, 2, 15));
        assertFalse(resultOfSearching06.isPresent());

        var resultOfSearching07 = findEvent(dateTimeOf(2020, 4, 10), null);
        assertTrue(resultOfSearching07.isPresent());

        var resultOfSearching08 = findEvent(dateTimeOf(2020, 7, 10), null);
        assertFalse(resultOfSearching08.isPresent());

        var resultOfSearching09 = findEvent(null, dateTimeOf(2020, 7, 10));
        assertTrue(resultOfSearching09.isPresent());

        var resultOfSearching10 = findEvent(null, dateTimeOf(2020, 1, 10));
        assertFalse(resultOfSearching10.isPresent());

        var resultOfSearching11 = findEvent(null, null);
        assertTrue(resultOfSearching11.isPresent());

        var resultOfSearching12 = findEvent(dateTimeOf(2020, 5, 10), null);
        assertTrue(resultOfSearching12.isPresent());

        var resultOfSearching13 = findEvent(null, dateTimeOf(2020, 5, 10));
        assertTrue(resultOfSearching13.isPresent());
    }

    @Test
    public void checkPagination() throws IOException {
        //GIVEN
        var feed = feedDao.getFeeds().stream()
                .filter(f -> f.getAlias().equals(feedAlias))
                .findFirst()
                .orElseThrow();

        var earliestEvent = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);
        earliestEvent.setUpdatedAt(OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 13, 22), ZoneOffset.UTC));
        earliestEvent.setEnriched(true);
        feedDao.insertFeedData(earliestEvent, feedAlias);

        var middleEvent = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);
        middleEvent.setUpdatedAt(OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 13, 23), ZoneOffset.UTC));
        middleEvent.setEnriched(true);
        feedDao.insertFeedData(middleEvent, feedAlias);

        var latestEvent = new FeedData(UUID.randomUUID(), feed.getFeedId(), 1L);
        latestEvent.setUpdatedAt(OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 13, 24), ZoneOffset.UTC));
        latestEvent.setEnriched(true);
        feedDao.insertFeedData(latestEvent, feedAlias);

        //when page 1 ASC
        List<TestEventDto> page1Asc = objectMapper.readValue(eventResourceService
                .searchEvents(feedAlias, null, null, null, null, 2, null, ASC, null, ANY, GeometryFilterType.ANY).get(), TestEventListDto.class).getData();

        //then
        assertEquals(2, page1Asc.size());
        assertEquals(earliestEvent.getEventId(), page1Asc.get(0).getEventId());
        assertEquals(middleEvent.getEventId(), page1Asc.get(1).getEventId());

        //when page 2 ASC
        List<TestEventDto> page2Asc = objectMapper.readValue(eventResourceService
                .searchEvents(feedAlias, null, null, null, middleEvent.getUpdatedAt(), 2, null, ASC, null, ANY, GeometryFilterType.ANY).get(), TestEventListDto.class).getData();

        //then
        assertEquals(1, page2Asc.size());
        assertEquals(latestEvent.getEventId(), page2Asc.get(0).getEventId());

        //when page 1 DESC
        List<TestEventDto> page1Desc = objectMapper.readValue(eventResourceService
                .searchEvents(feedAlias, null, null, null, null, 2, null, DESC, null, ANY, GeometryFilterType.ANY).get(), TestEventListDto.class).getData();

        //then
        assertEquals(2, page1Desc.size());
        assertEquals(latestEvent.getEventId(), page1Desc.get(0).getEventId());
        assertEquals(middleEvent.getEventId(), page1Desc.get(1).getEventId());

        //when page 2 DESC
        List<TestEventDto> page2Desc = objectMapper.readValue(eventResourceService
                .searchEvents(feedAlias, null, null, null, middleEvent.getUpdatedAt(), 2, null, DESC, null, ANY, GeometryFilterType.ANY).get(), TestEventListDto.class).getData();

        //then
        assertEquals(1, page2Desc.size());
        assertEquals(earliestEvent.getEventId(), page2Desc.get(0).getEventId());
    }

    private OffsetDateTime dateTimeOf(int year, int month, int day) {
        return OffsetDateTime.of(LocalDateTime.of(year, month, day, 0, 0), ZoneOffset.UTC);
    }

    private Optional<TestEventDto> findEvent(OffsetDateTime after, OffsetDateTime before) throws IOException {
        List<TestEventDto> iterable = fetchEvent(after, before);
        return iterable.isEmpty() ? Optional.empty() : Optional.of(Iterables.getOnlyElement(iterable));
    }

    private List<TestEventDto> fetchEvent(OffsetDateTime from, OffsetDateTime to) throws IOException {
        Optional<String> response = eventResourceService.searchEvents(
                feedAlias, List.of(), from, to, null, 1, List.of(), ASC, null, ANY, GeometryFilterType.ANY);
        if (response.isPresent()) {
            return objectMapper.readValue(response.get(), TestEventListDto.class).getData();
        }
        return emptyList();
    }

    private List<TestEventDto> fetchEvent(List<BigDecimal> bbox) throws IOException {
        Optional<String> response = eventResourceService.searchEvents(
                feedAlias, List.of(), null, null, null, 1, List.of(), ASC, bbox, ANY, GeometryFilterType.ANY);
        if (response.isPresent()) {
            return objectMapper.readValue(response.get(), TestEventListDto.class).getData();
        }
        return emptyList();
    }
}
