package io.kontur.eventapi.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootResource {

    @GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(tags = "Service", summary = "Simple service check")
    @ApiResponse(responseCode = "200", description = "Service is up")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("OK");
    }
}
