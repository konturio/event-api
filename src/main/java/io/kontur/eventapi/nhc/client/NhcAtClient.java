package io.kontur.eventapi.nhc.client;

import io.kontur.eventapi.cap.client.CapImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "nhcAtClient", url = "${nhc.host}")
public interface NhcAtClient extends CapImportClient {

    @GetMapping("/index-at.xml")
    String getXml();
}
