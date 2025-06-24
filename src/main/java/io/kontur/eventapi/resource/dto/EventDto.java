package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import lombok.Data;
import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

@Data
public class EventDto {
    private UUID eventId;
    private Long version;
    private String name;
    private String properName;
    private String description;
    private EventType type;
    private Severity severity;
    private Boolean active;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private String location;
    private List<String> urls = new ArrayList<>();
    private Map<String, Object> loss = new HashMap<>();
    private Map<String, Object> severityData = new HashMap<>();
    private Map<String, Object> eventDetails;
    private Set<UUID> observations = new HashSet<>();
    private FeatureCollection geometries;
    private List<EpisodeDto> episodes = new ArrayList<>();
    private List<Double> bbox = new ArrayList<>();
    private List<Double> centroid = new ArrayList<>();
    private int episodeCount;
}
