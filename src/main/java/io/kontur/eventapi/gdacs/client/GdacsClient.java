package io.kontur.eventapi.gdacs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "gdacsFeignClient", url = "${gdacs.host}")
public interface GdacsClient {

    @GetMapping("/xml/gdacs_cap.xml")
    String getXml();

    @GetMapping("/gdacsapi/api/polygons/getgeometry")
    String getGeometryByLink(@RequestParam("eventtype") String eventtype, @RequestParam("eventid") String eventid, @RequestParam("episodeid") String episodeid);
}
