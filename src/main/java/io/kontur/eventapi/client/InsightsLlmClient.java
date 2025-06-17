package io.kontur.eventapi.client;

import io.kontur.eventapi.embedding.dto.EmbeddingRequest;
import io.kontur.eventapi.embedding.dto.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Qualifier("insightsLlmClient")
@FeignClient(value = "insightsLlmClient", url = "${insightsLlmApi.host}")
public interface InsightsLlmClient {

    @PostMapping(value = "/embeddings", consumes = "application/json")
    @ResponseBody
    EmbeddingResponse embedding(@RequestBody EmbeddingRequest request);
}
