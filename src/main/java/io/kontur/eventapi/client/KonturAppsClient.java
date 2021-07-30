package io.kontur.eventapi.client;

import io.kontur.eventapi.enrichment.InsightsApiRequest;
import io.kontur.eventapi.enrichment.InsightsApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value = "konturAppsClient", url = "${konturApps.host}")
public interface KonturAppsClient {
    @PostMapping(value = "/insights-api/graphql", headers = "Content-Type=application/json")
    @ResponseBody
    InsightsApiResponse graphql(@RequestBody InsightsApiRequest request);
}
