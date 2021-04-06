package io.kontur.eventapi.staticdata.config;

import java.time.OffsetDateTime;

public class StaticFileData {
    private final String path;
    private final String provider;
    private final OffsetDateTime updatedAt;
    private final String type;

    public StaticFileData(String path, String provider, OffsetDateTime updatedAt, String type) {
        this.path = path;
        this.provider = provider;
        this.updatedAt = updatedAt;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public String getProvider() {
        return provider;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getType() {
        return type;
    }
}
