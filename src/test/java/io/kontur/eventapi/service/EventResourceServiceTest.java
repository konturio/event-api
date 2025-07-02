package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.resource.dto.GeometryFilterType;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EventResourceServiceTest {
    @Test
    public void findSimilarEventsDelegatesToDao() {
        ApiDao apiDao = mock(ApiDao.class);
        Environment env = mock(Environment.class);
        EventResourceService service = new EventResourceService(apiDao, env);

        UUID eventId = UUID.randomUUID();
        String feed = "test";
        when(apiDao.findSimilarEvents(eventId, feed, 5, 10.0,
                EpisodeFilterType.ANY, GeometryFilterType.NONE))
                .thenReturn("{}");

        Optional<String> result = service.findSimilarEvents(eventId, feed, 5, 10.0,
                EpisodeFilterType.ANY, GeometryFilterType.NONE);

        verify(apiDao, times(1)).findSimilarEvents(eventId, feed, 5, 10.0,
                EpisodeFilterType.ANY, GeometryFilterType.NONE);
        assertTrue(result.isPresent());
    }
}
