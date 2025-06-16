package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.MergeOperationsMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class MergeOperationsDao {

    private final MergeOperationsMapper mapper;

    public MergeOperationsDao(MergeOperationsMapper mapper) {
        this.mapper = mapper;
    }

    public OffsetDateTime getLastApprovedAt() {
        return mapper.getLastApprovedAt();
    }

    @Transactional
    public void updateMergeDecision(UUID eventId1, UUID eventId2, Boolean approved,
                                    String decisionMadeBy, OffsetDateTime decisionMadeAt) {
        mapper.updateMergeDecision(eventId1, eventId2, approved, decisionMadeBy, decisionMadeAt);
    }
}
