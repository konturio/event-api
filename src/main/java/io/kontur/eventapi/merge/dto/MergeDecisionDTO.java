package io.kontur.eventapi.merge.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class MergeDecisionDTO {
    private Boolean approved;
    private String decisionMadeBy;
    private UUID eventId1;
    private UUID eventId2;
}
