package io.kontur.eventapi.emdat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(value = "emDatClient", url = "${emdat.host}")
public interface EmDatClient {

    @PostMapping("/graphql")
    String graphqlApi(@RequestBody String body, @RequestHeader Map<String, String> headers);

    @GetMapping("/files/{fileName}")
    ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName,
                                                   @RequestHeader Map<String, String> headers);
}
