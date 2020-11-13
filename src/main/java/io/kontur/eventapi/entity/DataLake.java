package io.kontur.eventapi.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DataLake {

    private UUID observationId;
    private String externalId;
    private OffsetDateTime updatedAt;
    private OffsetDateTime loadedAt;
    private String provider;
    private String data;

    public DataLake() {
    }

    public DataLake(UUID observationId, String externalId, OffsetDateTime updatedAt, OffsetDateTime loadedAt) {
        this.observationId = observationId;
        this.externalId = externalId;
        this.updatedAt = updatedAt;
        this.loadedAt = loadedAt;
    }

    public UUID getObservationId() {
        return observationId;
    }

    public void setObservationId(UUID observationId) {
        this.observationId = observationId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(OffsetDateTime loadedAt) {
        this.loadedAt = loadedAt;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
