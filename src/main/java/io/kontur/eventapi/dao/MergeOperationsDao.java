package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.MergeOperationsMapper;
import io.kontur.eventapi.entity.MergeOperation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MergeOperationsDao {
    private final MergeOperationsMapper mapper;

    public MergeOperationsDao(MergeOperationsMapper mapper) {
        this.mapper = mapper;
    }

    public List<MergeOperation> getPendingOperations() {
        return mapper.getPendingOperations();
    }

    public void markExecuted(UUID operationId) {
        mapper.markExecuted(operationId);
    }
}
