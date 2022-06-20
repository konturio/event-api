package io.kontur.eventapi.gdacs.client;

import io.kontur.eventapi.cap.client.CapImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "gdacsFeignClient", url = "${gdacs.host}")
public interface GdacsClient extends CapImportClient {

    @GetMapping("/xml/gdacs_cap.xml")
    String getXml();

    @GetMapping("/gdacsapi/api/polygons/getgeometry")
    String getGeometryByLink(@RequestParam("eventtype") String eventtype, @RequestParam("eventid") String eventid, @RequestParam("episodeid") String episodeid);
}
