package io.kontur.eventapi.inciweb.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "inciWebClient", url = "${inciweb.host}")
public interface InciWebClient {
    @GetMapping("/feeds/rss/incidents/")
    String getXml();
}
