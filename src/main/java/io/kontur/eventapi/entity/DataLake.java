package io.kontur.eventapi.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class DataLake {

    private UUID observationId;
    private String externalId;
    private OffsetDateTime updatedAt;
    private OffsetDateTime loadedAt;
    private String provider;
    private String data;
    private boolean normalized = false;
    private boolean skipped = false;

    public DataLake(UUID observationId, String externalId, OffsetDateTime updatedAt, OffsetDateTime loadedAt) {
        this.observationId = observationId;
        this.externalId = externalId;
        this.updatedAt = updatedAt;
        this.loadedAt = loadedAt;
    }
}
