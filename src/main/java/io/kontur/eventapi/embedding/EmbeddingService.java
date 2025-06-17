package io.kontur.eventapi.embedding;

import io.kontur.eventapi.client.InsightsLlmClient;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.embedding.dto.EmbeddingRequest;
import io.kontur.eventapi.entity.FeedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmbeddingService {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingService.class);

    private final InsightsLlmClient client;
    private final FeedDao feedDao;
    private final boolean enabled;

    public EmbeddingService(InsightsLlmClient client, FeedDao feedDao,
                            @Value("${embeddings.enabled:true}") boolean enabled) {
        this.client = client;
        this.feedDao = feedDao;
        this.enabled = enabled;
    }

    public void updateEmbedding(FeedData data) {
        if (!enabled) {
            return;
        }
        String text = data.getDescription() != null ? data.getDescription() : data.getName();
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            List<Double> vector = client.embedding(new EmbeddingRequest(text)).getEmbedding();
            if (vector != null && !vector.isEmpty()) {
                feedDao.updateEmbedding(data.getFeedId(), data.getEventId(), data.getVersion(), vector);
            }
        } catch (Exception e) {
            LOG.debug("Failed to obtain embedding for event {}: {}", data.getEventId(), e.getMessage());
        }
    }
}
