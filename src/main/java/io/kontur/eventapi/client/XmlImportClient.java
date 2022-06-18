package io.kontur.eventapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "xmlFeignClient", url = "www.example.com")
public interface XmlImportClient {

    @GetMapping("/cap.xml")
    String getXml();

    @GetMapping("/geom")
    String getGeometryByLink(@RequestParam("eventtype") String eventtype, @RequestParam("eventid") String eventid, @RequestParam("episodeid") String episodeid);

}
