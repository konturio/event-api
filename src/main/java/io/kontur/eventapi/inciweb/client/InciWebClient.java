package io.kontur.eventapi.inciweb.client;

import io.kontur.eventapi.client.XmlImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "inciWebClient", url = "${inciweb.host}")
public interface InciWebClient extends XmlImportClient {
    @GetMapping("/feeds/rss/incidents/")
    String getXml();
}
