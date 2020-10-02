package io.kontur.eventapi.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedData {

    private UUID eventId;
    private UUID feedId;
    private Long version;
    private String name;
    private String description;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime updatedBySourceAt;
    private List<UUID> observations = new ArrayList<>();
    private List<FeedEpisode> episodes = new ArrayList<>();

    public FeedData(UUID eventId, UUID feedId, Long version) {
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

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public OffsetDateTime getUpdatedBySourceAt() {
        return updatedBySourceAt;
    }

    public void setUpdatedBySourceAt(OffsetDateTime updatedBySourceAt) {
        this.updatedBySourceAt = updatedBySourceAt;
    }

    public List<UUID> getObservations() {
        return observations;
    }

    public void setObservations(List<UUID> observations) {
        this.observations = observations;
    }

    public List<FeedEpisode> getEpisodes() {
        return episodes;
    }

    public void addEpisode(FeedEpisode episode) {
        episodes.add(episode);
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FeedData{" +
                "eventId=" + eventId +
                ", feedId=" + feedId +
                ", version=" + version +
                ", startedAt=" + startedAt +
                ", endedAt=" + endedAt +
                ", updatedAt=" + updatedAt +
                ", updatedBySourceAt=" + updatedBySourceAt +
                ", observations=" + observations +
                ", episodes=" + episodes +
                '}';
    }
}
