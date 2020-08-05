package io.kontur.eventapi.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CombinedEpisodeDto {

    private Long id;
    private Long eventId;
    private UUID observationId;
    private String description;
    private String provider;
    private OffsetDateTime occurredOn;
    private OffsetDateTime loadedOn;
    private List<CombinedAreaDto> areas = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public UUID getObservationId() {
        return observationId;
    }

    public void setObservationId(UUID observationId) {
        this.observationId = observationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OffsetDateTime getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(OffsetDateTime occurredOn) {
        this.occurredOn = occurredOn;
    }

    public OffsetDateTime getLoadedOn() {
        return loadedOn;
    }

    public void setLoadedOn(OffsetDateTime loadedOn) {
        this.loadedOn = loadedOn;
    }

    public List<CombinedAreaDto> getAreas() {
        return areas;
    }

    public void addArea(CombinedAreaDto areaDto) {
        areas.add(areaDto);
    }
}
