package io.kontur.eventapi.resource.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class MergeCandidatePairDTO {
    private String eventId1;
    private String eventId2;
    private Double confidence;
    private Boolean approved;
    private String decisionMadeBy;
    private OffsetDateTime decisionMadeAt;
    private Map<String, Object> event1;
    private Map<String, Object> event2;
}
