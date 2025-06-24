package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.MergeOperationsMapper;
import io.kontur.eventapi.entity.MergeOperation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class MergeOperationsDao {
    private final MergeOperationsMapper mapper;

    public MergeOperationsDao(MergeOperationsMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<MergeOperation> getByEventIds(List<String> eventIds) {
        return Optional.ofNullable(mapper.getByEventIds(eventIds));
    }

    @Transactional
    public Optional<MergeOperation> takeNext() {
        MergeOperation op = mapper.selectNext();
        if (op != null) {
            mapper.markTaken(op.getMergeOperationId());
        }
        return Optional.ofNullable(op);
    }
}
