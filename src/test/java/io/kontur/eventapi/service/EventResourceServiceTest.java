package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EventResourceServiceTest {
    @Test
    public void searchByEmbeddingDelegatesToDao() {
        ApiDao apiDao = mock(ApiDao.class);
        Environment env = mock(Environment.class);
        when(apiDao.searchByEmbedding("feed", List.of(1.0), 10, EpisodeFilterType.NONE)).thenReturn("[]");

        EventResourceService service = new EventResourceService(apiDao, env);
        Optional<String> result = service.searchByEmbedding("feed", List.of(1.0), 10, EpisodeFilterType.NONE);

        assertTrue(result.isPresent());
        verify(apiDao).searchByEmbedding("feed", List.of(1.0), 10, EpisodeFilterType.NONE);
    }
}

