package io.kontur.eventapi.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KonturEvent {
    private UUID eventId;
    private List<UUID> observationIds = new ArrayList<>();

    public KonturEvent(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public List<UUID> getObservationIds() {
        return observationIds;
    }

    public KonturEvent setObservationIds(List<UUID> observationIds) {
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
