package io.kontur.eventapi.gdacs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "gdacsBasicAuth", url ="${gdacs.host}")
public interface GdacsClient {

    @GetMapping
    String getXml();
}
