package io.kontur.eventapi.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RootResourceTest {

    private RootResource rootResource;

    @BeforeEach
    public void setUp() {
        rootResource = new RootResource();
    }

    @Test
    public void pingReturnsOk() {
        ResponseEntity<String> resp = rootResource.index();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("OK", resp.getBody());
    }
}
