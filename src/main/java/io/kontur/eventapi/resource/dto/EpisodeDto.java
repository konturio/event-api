package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.Severity;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.*;
import org.wololo.geojson.FeatureCollection;

@Data
public class EpisodeDto {
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
}
