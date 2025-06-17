package io.kontur.eventapi.emdat.classification;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmDatTypeClassifier {

    private static final Logger LOG = LoggerFactory.getLogger(EmDatTypeClassifier.class);
    private static final String CLASSIFICATION_FILE = "emdat-classification.json";

    private Map<String, EventType> map = Collections.emptyMap();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CLASSIFICATION_FILE)) {
            if (is == null) {
                LOG.warn("Classification file {} not found", CLASSIFICATION_FILE);
                return;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> raw = JsonUtil.readJson(json, new TypeReference<>() {});
            map = raw.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> EventType.valueOf(e.getValue())));
        } catch (IOException e) {
            LOG.error("Failed to load EM-Dat classification", e);
        }
    }

    public EventType classify(String name) {
        return map.getOrDefault(name, EventType.OTHER);
    }
}
