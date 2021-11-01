package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.entity.FeedEpisode;
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
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private Set<UUID> observations;
    private List<FeedEpisode> episodes = new ArrayList<>();
    private Map<String, Object> eventDetails = new HashMap<>();
    private FeatureCollection geometries;
    private List<String> urls = new ArrayList<>();
    private String location;
}
