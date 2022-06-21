package io.kontur.eventapi.nhc.client;

import io.kontur.eventapi.cap.client.CapImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "nhcCpClient", url = "${nhc.host}")
public interface NhcCpClient extends CapImportClient {

    @GetMapping("/index-cp.xml")
    String getXml();
}
