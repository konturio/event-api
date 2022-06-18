package io.kontur.eventapi.nhc.client;

import io.kontur.eventapi.client.XmlImportClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "nhcCpClient", url = "${nhc.host}")
public interface NhcCpClient extends XmlImportClient {

    @GetMapping("/index-cp.xml")
    String getXml();
}
