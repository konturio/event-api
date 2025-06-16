package io.kontur.eventapi.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class MergeOperation {
    private UUID operationId;
    private UUID eventId1;
    private UUID eventId2;
    private Boolean approved;
    private Boolean executed;
}
