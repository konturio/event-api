package io.kontur.eventapi.entity;

import org.wololo.geojson.FeatureCollection;

import java.time.OffsetDateTime;
import java.util.*;

public class FeedData {

    private UUID eventId;
    private UUID feedId;
    private Long version;
    private String name;
    private String description;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime updatedAt;
    private Set<UUID> observations = new HashSet<>();
    private List<FeedEpisode> episodes = new ArrayList<>();
    private FeatureCollection geometries;
    private Map<String, Object> eventDetails;
    private Boolean enriched;
    private Long enrichmentAttempts;
    private Boolean enrichmentSkipped;
    private List<String> urls = new ArrayList<>();

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

    public Set<UUID> getObservations() {
        return observations;
    }

    public void setObservations(Set<UUID> observations) {
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

    public Map<String, Object> getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(Map<String, Object> eventDetails) {
        this.eventDetails = eventDetails;
    }

    public Boolean getEnriched() {
        return enriched;
    }

    public void setEnriched(Boolean enriched) {
        this.enriched = enriched;
    }

    public FeatureCollection getGeometries() {
        return geometries;
    }

    public void setGeometries(FeatureCollection geometries) {
        this.geometries = geometries;
    }

    public Long getEnrichmentAttempts() {
        return enrichmentAttempts;
    }

    public void setEnrichmentAttempts(Long enrichmentAttempts) {
        this.enrichmentAttempts = enrichmentAttempts;
    }

    public Boolean getEnrichmentSkipped() {
        return enrichmentSkipped;
    }

    public void setEnrichmentSkipped(Boolean enrichmentSkipped) {
        this.enrichmentSkipped = enrichmentSkipped;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
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
                ", observations=" + observations +
                ", episodes=" + episodes +
                ", eventDetails=" + eventDetails +
                ", enriched=" + enriched +
                '}';
    }
}
