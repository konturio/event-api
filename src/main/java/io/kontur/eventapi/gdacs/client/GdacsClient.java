package io.kontur.eventapi.gdacs.client;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "gdacsBasicAuth", url = "${gdacs.host}")
public interface GdacsClient {

    @GetMapping("/xml/gdacs_cap.xml")
    String getXml();

    @GetMapping("{link}")
    @Headers("Content-Type: text/xml")
    String getAlertByLink(@PathVariable("link") String link);
}
