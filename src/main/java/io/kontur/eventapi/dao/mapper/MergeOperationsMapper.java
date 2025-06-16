package io.kontur.eventapi.dao.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper
public interface MergeOperationsMapper {

    OffsetDateTime getLastApprovedAt();

    void updateMergeDecision(@Param("eventId1") UUID eventId1,
                             @Param("eventId2") UUID eventId2,
                             @Param("approved") Boolean approved,
                             @Param("decisionMadeBy") String decisionMadeBy,
                             @Param("decisionMadeAt") OffsetDateTime decisionMadeAt);
}
