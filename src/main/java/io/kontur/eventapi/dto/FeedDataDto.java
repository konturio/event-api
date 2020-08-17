package io.kontur.eventapi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedDataDto {

    private UUID eventId;
    private UUID feedId;
    private Long version;
    private String name;
    private String description;
    private List<UUID> observations = new ArrayList<>();
    private List<FeedEpisodeDto> episodes = new ArrayList<>();

    public FeedDataDto(UUID eventId, UUID feedId, Long version) {
        this.eventId = eventId;
        this.feedId = feedId;
        this.version = version;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getFeedId() {
        return feedId;
    }

    public void setFeedId(UUID feedId) {
        this.feedId = feedId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public List<UUID> getObservations() {
        return observations;
    }

    public void setObservations(List<UUID> observations) {
        this.observations = observations;
    }

    public List<FeedEpisodeDto> getEpisodes() {
        return episodes;
    }

    public void addEpisode(FeedEpisodeDto episode) {
        episodes.add(episode);
    }
}
