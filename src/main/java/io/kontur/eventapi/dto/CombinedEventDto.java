package io.kontur.eventapi.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CombinedEventDto {

    private Long id;
    private UUID observationId;
    private EventType type;
    private String name;
    private String description;
    private OffsetDateTime startedOn;
    private OffsetDateTime endedOn;
    private List<CombinedEpisodeDto> episodes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getObservationId() {
        return observationId;
    }

    public void setObservationId(UUID observationId) {
        this.observationId = observationId;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(OffsetDateTime startedOn) {
        this.startedOn = startedOn;
    }

    public OffsetDateTime getEndedOn() {
        return endedOn;
    }

    public void setEndedOn(OffsetDateTime endedOn) {
        this.endedOn = endedOn;
    }

    public List<CombinedEpisodeDto> getEpisodes() {
        return episodes;
    }

    public void addEpisode(CombinedEpisodeDto episodeDto) {
        episodes.add(episodeDto);
    }

}
