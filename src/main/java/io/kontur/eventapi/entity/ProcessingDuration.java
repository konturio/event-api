package io.kontur.eventapi.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProcessingDuration {
    private Double avg;
    private Double max;
    private Double min;
    private Integer count;
    private OffsetDateTime latestProcessedAt;
}
