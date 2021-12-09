package io.kontur.eventapi.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PgStatTable {
    private String tableName;
    private OffsetDateTime lastVacuum;
    private OffsetDateTime lastAutoVacuum;
    private OffsetDateTime lastAnalyse;
    private OffsetDateTime lastAutoAnalyze;
    private Long liveTupCount;
    private Long deadTupCount;
    private Long vacuumCount;
    private Long autoVacuumCount;
    private Long analyzeCount;
    private Long autoAnalyzeCount;
}
