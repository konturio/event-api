package io.kontur.eventapi.crossprovidermerge.dto;

import lombok.Data;

@Data
public class MergePairDecisionDto {
    private Boolean approved;
    private String decisionMadeBy;
    private String eventId1;
    private String eventId2;
}
