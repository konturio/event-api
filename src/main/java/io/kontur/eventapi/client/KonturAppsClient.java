package io.kontur.eventapi.client;

import io.kontur.eventapi.enrichment.dto.InsightsApiRequest;
import io.kontur.eventapi.enrichment.dto.InsightsApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Qualifier("konturAppsClient")
@FeignClient(value = "konturAppsClient", url = "${konturApps.host}")
public interface KonturAppsClient {
    @PostMapping(value = "/insights/graphql", headers = "Content-Type=application/json")
    @ResponseBody
    InsightsApiResponse graphql(@RequestBody InsightsApiRequest request);
}
