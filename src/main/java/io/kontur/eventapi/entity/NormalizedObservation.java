package io.kontur.eventapi.entity;

import lombok.Data;
import org.wololo.geojson.FeatureCollection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class NormalizedObservation {

    private UUID observationId;
    private String externalEventId;
    private String externalEpisodeId;
    private String provider;
    private String point;
    private FeatureCollection geometries;
    private Severity eventSeverity;
    private String name;
    private String properName;
    private String description;
    private String episodeDescription;
    private EventType type;
    private Boolean active;
    private BigDecimal cost;
    private String region;
    private OffsetDateTime loadedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime sourceUpdatedAt;
    private String sourceUri;
    private List<String> urls = new ArrayList<>();
    private Boolean recombined = false;
    private OffsetDateTime normalizedAt;
}
