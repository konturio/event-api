package io.kontur.eventapi.jtwc.service;

import feign.FeignException;
import io.kontur.eventapi.jtwc.client.JtwcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JtwcService {

    private static final Logger LOG = LoggerFactory.getLogger(JtwcService.class);

    private final JtwcClient client;

    public JtwcService(JtwcClient client) {
        this.client = client;
    }

    public Optional<String> fetchFeed() {
        try {
            return Optional.of(client.getFeed());
        } catch (FeignException e) {
            LOG.warn("JTWC feed has not been received");
        }
        return Optional.empty();
    }

    public Optional<String> fetchProduct(String fileName) {
        try {
            return Optional.of(client.getProduct(fileName));
        } catch (FeignException e) {
            LOG.warn("JTWC product {} has not been received", fileName);
        }
        return Optional.empty();
    }
}
