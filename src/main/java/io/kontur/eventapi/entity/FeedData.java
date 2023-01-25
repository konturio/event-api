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
    private EventType type;
    private Severity severity;
    private String description;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private Set<UUID> observations = new HashSet<>();
    private List<FeedEpisode> episodes = new ArrayList<>();
    private FeatureCollection geometries;
    private Integer geomFuncType;
    private Map<String, Object> eventDetails;
    private Boolean enriched;
    private Long enrichmentAttempts;
    private Boolean enrichmentSkipped;
    private List<String> urls = new ArrayList<>();
    private String location;
    private Map<String, Object> loss;
    private OffsetDateTime composedAt;
    private OffsetDateTime enrichedAt;
    private List<Double> centroid = new ArrayList<>();
    private List<Double> bbox = new ArrayList<>();

    public FeedData(UUID eventId, UUID feedId, Long version) {
        this.eventId = eventId;
        this.feedId = feedId;
        this.version = version;
    }

    public void addEpisode(FeedEpisode episode) {
        episodes.add(episode);
    }
}
