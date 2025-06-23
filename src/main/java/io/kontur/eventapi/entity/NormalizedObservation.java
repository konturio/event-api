package io.kontur.eventapi.entity;

import lombok.Data;
import org.wololo.geojson.FeatureCollection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Data
public class NormalizedObservation {

    private UUID observationId;
    private String externalEventId;
    private String externalEpisodeId;
    private String provider;
    private String origin;
    private String name;
    private String properName;
    private String description;
    private String episodeDescription;
    private EventType type;
    private Severity eventSeverity;
    private Boolean active;
    private OffsetDateTime loadedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime sourceUpdatedAt;
    private String region;
    private List<String> urls = new ArrayList<>();
    private BigDecimal cost;
    private Map<String, Object> loss = new HashMap<>();
    private Map<String, Object> severityData = new HashMap<>();
    private FeatureCollection geometries;
    private Boolean autoExpire;
    private Boolean recombined = false;
    private OffsetDateTime normalizedAt;
}
