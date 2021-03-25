package io.kontur.eventapi.tornado.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "noaaClient", url = "${noaaTornado.host}")
public interface NoaaTornadoClient {

    @GetMapping("{filename}")
    byte[] getGZIP(@PathVariable("filename") String filename);
}
