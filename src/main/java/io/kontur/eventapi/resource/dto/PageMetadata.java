package io.kontur.eventapi.resource.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PageMetadata {
    private final OffsetDateTime nextAfterValue;

    public PageMetadata(OffsetDateTime nextAfterValue) {
        this.nextAfterValue = nextAfterValue;
    }
}
