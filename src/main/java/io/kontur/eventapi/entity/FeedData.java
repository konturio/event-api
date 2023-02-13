package io.kontur.eventapi.entity;

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
    private EventType type;
    private Severity severity;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private String location;
    private List<String> urls = new ArrayList<>();
    private Map<String, Object> eventDetails;
    private Set<UUID> observations = new HashSet<>();
    private FeatureCollection geometries;
    private List<FeedEpisode> episodes = new ArrayList<>();
    private List<Double> centroid = new ArrayList<>();
    private List<Double> bbox = new ArrayList<>();
    private Boolean enriched;
    private Long enrichmentAttempts;
    private Boolean enrichmentSkipped;
    private OffsetDateTime composedAt;
    private OffsetDateTime enrichedAt;
    private Integer geomFuncType;

    public FeedData(UUID eventId, UUID feedId, Long version) {
        this.eventId = eventId;
        this.feedId = feedId;
        this.version = version;
    }

    public void addEpisode(FeedEpisode episode) {
        episodes.add(episode);
    }
}
