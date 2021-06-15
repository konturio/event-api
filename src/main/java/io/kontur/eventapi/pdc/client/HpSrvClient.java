package io.kontur.eventapi.pdc.client;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Headers;
import io.kontur.eventapi.pdc.config.FeignClientConfiguration;
import io.kontur.eventapi.pdc.dto.HpSrvSearchBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "pdcHpSrvBasicAuth", url ="${pdc.host}", configuration = FeignClientConfiguration.class)
public interface HpSrvClient {

    @PostMapping("/hp_srv/services/hazards/1/json/search_hazard")
    @Headers({"Content-Type: application/json", "accept: application/json"})
    JsonNode searchHazards(@RequestBody HpSrvSearchBody body);

    @GetMapping("/hp_srv/services/mags/1/json/get_mags")
    JsonNode getMags(@RequestParam("hazard_id") String hazardId);
}
