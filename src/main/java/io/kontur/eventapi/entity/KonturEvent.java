package io.kontur.eventapi.entity;

import java.util.*;

public class KonturEvent {
    private UUID eventId;
    private Set<UUID> observationIds = new HashSet<>();

    public KonturEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Set<UUID> getObservationIds() {
        return observationIds;
    }

    public KonturEvent setObservationIds(Set<UUID> observationIds) {
        this.observationIds = observationIds;
        return this;
    }

    public void addObservations(UUID observations) {
        this.observationIds.add(observations);
    }

    @Override
    public String toString() {
        return "KonturEvent{" +
                "eventId=" + eventId +
                ", observationIds=" + observationIds +
                '}';
    }
}
