package io.kontur.eventapi.nhc.client;

import io.kontur.eventapi.cap.client.CapImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "nhcEpClient", url = "${nhc.host}")
public interface NhcEpClient extends CapImportClient {

    @GetMapping("/index-ep.xml")
    String getXml();
}
