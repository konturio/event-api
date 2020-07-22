package io.kontur.eventapi.pdc.client;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Headers;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pdcHpSrvBasicAuth", url ="${pdcHpSrv.host}")
public interface HpSrvClient {

    @PostMapping("/services/hazards/1/json/search_hazard")
    @Headers({"Content-Type: application/json", "accept: application/json"})
    JsonNode searchForHazards(@RequestBody HpSrvSearchBody body);
}
