package io.kontur.eventapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

@Data
public class FeedData {

    private UUID eventId;
    private UUID feedId;
    private Long version;
    private String name;
    private String properName;
    private String description;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private Set<UUID> observations = new HashSet<>();
    private List<FeedEpisode> episodes = new ArrayList<>();
    private FeatureCollection geometries;
    private Map<String, Object> eventDetails;
    private Boolean enriched;
    private Long enrichmentAttempts;
    private Boolean enrichmentSkipped;
    private List<String> urls = new ArrayList<>();
    private String location;
    private OffsetDateTime composedAt;
    private OffsetDateTime enrichedAt;
    @JsonIgnore
    private Severity latestSeverity;
    @JsonIgnore
    private List<Severity> severities;

    public FeedData(UUID eventId, UUID feedId, Long version) {
        this.eventId = eventId;
        this.feedId = feedId;
        this.version = version;
    }

    public void addEpisode(FeedEpisode episode) {
        episodes.add(episode);
    }
}
