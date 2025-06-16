package io.kontur.eventapi.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class MergeOperation {
    private Long mergeOperationId;
    private List<String> eventIds;
    private Double confidence;
    private Boolean approved;
    private String decisionMadeBy;
    private Boolean executed;
    private OffsetDateTime decisionMadeAt;
    private OffsetDateTime takenToMergeAt;
}
