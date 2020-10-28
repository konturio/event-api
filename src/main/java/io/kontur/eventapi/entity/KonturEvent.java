package io.kontur.eventapi.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KonturEvent {

    private UUID eventId;
    private Long version;
    private List<UUID> observationIds = new ArrayList<>();
    private String provider;

    public KonturEvent(UUID eventId, Long version) {
        this.eventId = eventId;
        this.version = version;
    }

    public KonturEvent(UUID eventId, Long version, List<UUID> observationIds) {
        this.eventId = eventId;
        this.version = version;
        this.observationIds = observationIds;
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

    public void addObservations(UUID observations) {
        this.observationIds.add(observations);
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "KonturEvent{" +
                "eventId=" + eventId +
                ", version=" + version +
                ", observationIds=" + observationIds +
                ", provider='" + provider + '\'' +
                '}';
    }
}
