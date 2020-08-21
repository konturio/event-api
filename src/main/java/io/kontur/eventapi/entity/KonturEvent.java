package io.kontur.eventapi.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KonturEvent {

    private UUID eventId;
    private Long version;
    private List<UUID> observationIds = new ArrayList<>();

    public KonturEvent(UUID eventId, Long version) {
        this.eventId = eventId;
        this.version = version;
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

    public void setObservationIds(List<UUID> observationIds) {
        this.observationIds = observationIds;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void addObservations(List<UUID> observations) {
        this.observationIds.addAll(observations);
    }
}
