package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.job.EventCombinationJob;
import io.kontur.eventapi.job.FeedCompositionJob;
import io.kontur.eventapi.job.NormalizationJob;
import io.kontur.eventapi.resource.dto.EventDto;
import io.kontur.eventapi.service.EventResourceService;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GdacsSearchJobIT extends AbstractIntegrationTest {

    private final GdacsSearchJob gdacsSearchJob;
    private final NormalizationJob normalizationJob;
    private final EventCombinationJob eventCombinationJob;
    private final FeedCompositionJob feedCompositionJob;
    private final EventResourceService eventResourceService;

    @Autowired
    public GdacsSearchJobIT(GdacsSearchJob gdacsSearchJob, NormalizationJob normalizationJob, EventCombinationJob eventCombinationJob, FeedCompositionJob feedCompositionJob, EventResourceService eventResourceService) {
        this.gdacsSearchJob = gdacsSearchJob;
        this.normalizationJob = normalizationJob;
        this.eventCombinationJob = eventCombinationJob;
        this.feedCompositionJob = feedCompositionJob;
        this.eventResourceService = eventResourceService;
    }


    @Test
    public void testSaveAlerts() {
        gdacsSearchJob.run();
        normalizationJob.run();
        eventCombinationJob.run();
        feedCompositionJob.run();

        List<EventDto> gdacsEvents = eventResourceService.searchEvents("gdacs", List.of(), OffsetDateTime.now().minusYears(1), 100, List.of(), SortOrder.DESC);

        assertFalse(gdacsEvents.isEmpty());

//        test existing geometry
        gdacsEvents.forEach(event -> {
            assertTrue(event.getEpisodes().stream().noneMatch(episode -> episode.getGeometries() == null));
        });


        List<List<FeedEpisode>> episodesWhereMoreThanTwo = gdacsEvents.stream()
                .filter(event -> event.getEpisodes().size() > 2)
                .map(EventDto::getEpisodes)
                .collect(toList());

        if (!episodesWhereMoreThanTwo.isEmpty()) {
//        test latest event
            episodesWhereMoreThanTwo.forEach(listEpisodes ->
                    assertTrue(listEpisodes.get(listEpisodes.size() - 1).getUpdatedAt().isAfter(listEpisodes.get(listEpisodes.size() - 2).getUpdatedAt()))
            );
        }
    }
}