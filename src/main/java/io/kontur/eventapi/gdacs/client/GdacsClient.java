package io.kontur.eventapi.gdacs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "gdacsFeignClient", url = "${gdacs.host}")
public interface GdacsClient {

    @GetMapping("/xml/gdacs_cap.xml")
    String getXml();

    @GetMapping("{link}")
    String getAlertByLink(@PathVariable("link") String link);
}
