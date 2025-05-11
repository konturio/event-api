package io.kontur.eventapi.entity;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

@Data
public class FeedEpisode {

    private String name;
    private String properName;
    private String description;
    private EventType type;
    private Severity severity;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime sourceUpdatedAt;
    private String location;
    private List<String> urls = new ArrayList<>();
    private Map<String, Object> loss = new HashMap<>();
    private Map<String, Object> severityData = new HashMap<>();
    private Map<String, Object> episodeDetails;
    private Set<UUID> observations = new HashSet<>();
    private FeatureCollection geometries;

    public void addUrlIfNotExists(String url) {
        if (isNotBlank(url) && !this.urls.contains(url)) {
            this.urls.add(url);
        }
    }

    public void addUrlIfNotExists(List<String> urls) {
        if (!CollectionUtils.isEmpty(urls)) {
            urls.stream().filter(url -> !this.urls.contains(url)).forEach(url -> this.urls.add(url));
        }
    }

    public void addObservation(UUID observationId) {
        observations.add(observationId);
    }

    public void addObservations(Set<UUID> observationIds) {
        observations.addAll(observationIds);
    }
}
