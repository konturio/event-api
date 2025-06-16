package io.kontur.eventapi.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.*;

@Data
public class KonturEvent {
    private UUID eventId;
    private String externalEventId;
    private Set<UUID> observationIds = new HashSet<>();
    private OffsetDateTime recombinedAt;

    public KonturEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public KonturEvent setObservationIds(Set<UUID> observationIds) {
        this.observationIds = observationIds;
        return this;
    }

    public void addObservations(UUID observations) {
        this.observationIds.add(observations);
    }
}
