package io.kontur.eventapi.resource.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MergeCandidatePairDTO {
    private String eventId1;
    private String eventId2;
    private Double confidence;
    private Boolean approved;
    private String decisionMadeBy;
    private OffsetDateTime decisionMadeAt;
    private EventDto event1;
    private EventDto event2;
}
