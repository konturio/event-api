package io.kontur.eventapi.usgs.earthquake.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "usgsEarthquakeClient", url = "${usgs.host}")
public interface UsgsEarthquakeClient {

    @GetMapping("/earthquakes/feed/v1.0/summary/4.5_week.geojson")
    String getEarthquakes();
}
