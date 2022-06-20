package io.kontur.eventapi.inciweb.client;

import io.kontur.eventapi.cap.client.CapImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "inciWebClient", url = "${inciweb.host}")
public interface InciWebClient extends CapImportClient {
    @GetMapping("/feeds/rss/incidents/")
    String getXml();
}
