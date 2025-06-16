package io.kontur.eventapi.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MergedGroup {
    private UUID mergeGroupId;
    private UUID eventId;
    private BigDecimal primaryIdx;
}
