package io.kontur.eventapi.entity;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

@Data
public class FeedEpisode {

    private UUID eventId;
    private UUID feedId;
    private Long version;
    private Long episodeNumber;
    private String name;
    private String properName;
    private String description;
    private EventType type;
    private Boolean active;
    private Severity severity;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime sourceUpdatedAt;
    private Set<UUID> observations = new HashSet<>();
    private Map<String, Object> episodeDetails;
    private List<String> urls = new ArrayList<>();
    private String location;
    private FeatureCollection geometries;
    private Boolean enriched;

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

    public void addObservation(UUID observations) {
        this.observations.add(observations);
    }
}
