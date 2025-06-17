package io.kontur.eventapi.embedding;

import io.kontur.eventapi.client.InsightsLlmClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.embedding.dto.EmbeddingRequest;
import io.kontur.eventapi.embedding.dto.EmbeddingResponse;
import io.kontur.eventapi.entity.FeedData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmbeddingServiceTest {

    private final InsightsLlmClient client = mock(InsightsLlmClient.class);
    private final FeedDao feedDao = mock(FeedDao.class);
    private final EmbeddingService service = new EmbeddingService(client, feedDao, true);

    @Test
    void updateEmbedding() {
        FeedData data = new FeedData(UUID.randomUUID(), UUID.randomUUID(), 1L);
        data.setDescription("test");
        EmbeddingResponse response = new EmbeddingResponse();
        response.setEmbedding(List.of(1.0, 2.0));
        when(client.embedding(any(EmbeddingRequest.class))).thenReturn(response);

        service.updateEmbedding(data);

        verify(client, times(1)).embedding(any(EmbeddingRequest.class));
        verify(feedDao, times(1)).updateEmbedding(eq(data.getFeedId()), eq(data.getEventId()), eq(1L), eq(List.of(1.0, 2.0)));
    }
}
