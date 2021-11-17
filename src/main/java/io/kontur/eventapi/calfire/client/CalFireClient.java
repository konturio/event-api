package io.kontur.eventapi.calfire.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "calFireClient", url = "${calfire.host}")
public interface CalFireClient {
    @GetMapping("/umbraco/api/IncidentApi/GeoJsonList?inactive=true")
    String getEvents();
}
