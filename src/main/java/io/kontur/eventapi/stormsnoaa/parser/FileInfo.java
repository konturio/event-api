package io.kontur.eventapi.stormsnoaa.parser;

import java.time.OffsetDateTime;

public class FileInfo {
    private final String filename;
    private final OffsetDateTime updatedAt;

    public FileInfo(String filename, OffsetDateTime updatedAt) {
        this.filename = filename;
        this.updatedAt = updatedAt;
    }

    public String getFilename() {
        return filename;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
