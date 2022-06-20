package io.kontur.eventapi.cap.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "capFeignClient")
public interface CapImportClient {

    @GetMapping("")
    String getXml();

    @GetMapping("")
    String getGeometryByLink(@RequestParam("eventtype") String eventtype, @RequestParam("eventid") String eventid, @RequestParam("episodeid") String episodeid);

}
