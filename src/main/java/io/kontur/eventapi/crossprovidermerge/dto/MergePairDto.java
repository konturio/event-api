package io.kontur.eventapi.crossprovidermerge.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MergePairDto {
    private String eventId1;
    private String eventId2;
    private Double confidence;
    private Boolean approved;
    private String decisionMadeBy;
    private OffsetDateTime decisionMadeAt;
    private Object event1;
    private Object event2;
}
