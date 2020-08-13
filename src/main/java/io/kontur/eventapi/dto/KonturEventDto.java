package io.kontur.eventapi.dto;

import java.util.UUID;

public class KonturEventDto {

    private UUID eventId;
    private UUID observationId;
    private Long version;

    public KonturEventDto(UUID eventId, Long version, UUID observationId) {
        this.eventId = eventId;
        this.version = version;
        this.observationId = observationId;
    }

    public KonturEventDto(UUID eventId, Long version) {
        this.eventId = eventId;
        this.version = version;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getObservationId() {
        return observationId;
    }

    public void setObservationId(UUID observationId) {
        this.observationId = observationId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
