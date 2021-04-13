package io.kontur.eventapi.stormsnoaa.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "noaaClient", url = "${stormsNoaa.host}")
public interface StormsNoaaClient {

    @GetMapping("{filename}")
    byte[] getGZIP(@PathVariable("filename") String filename);
}
