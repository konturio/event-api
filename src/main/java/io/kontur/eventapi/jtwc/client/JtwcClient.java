package io.kontur.eventapi.jtwc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jtwcClient", url = "${jtwc.host}")
public interface JtwcClient {

    @GetMapping("/jtwc/rss/jtwc.rss")
    String getFeed();

    @GetMapping("/jtwc/products/{fileName}")
    String getProduct(@PathVariable("fileName") String fileName);
}
