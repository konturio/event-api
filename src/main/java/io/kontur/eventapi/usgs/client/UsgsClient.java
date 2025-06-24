package io.kontur.eventapi.usgs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "usgsClient", url = "${usgs.host}")
public interface UsgsClient {

    @GetMapping("/fdsnws/event/1/query")
    String getShakeMapEvents(@RequestParam("format") String format,
                             @RequestParam("orderby") String orderby,
                             @RequestParam("producttype") String productType,
                             @RequestParam("limit") int limit,
                             @RequestParam("minmagnitude") Double minMagnitude);

    @GetMapping("/fdsnws/event/1/query")
    String getEvent(@RequestParam("eventid") String eventId,
                    @RequestParam("format") String format,
                    @RequestParam("producttype") String productType);
}
