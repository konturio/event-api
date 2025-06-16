package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.MergeOperationsMapper;
import io.kontur.eventapi.entity.MergeOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MergeOperationsDao {

    private final MergeOperationsMapper mapper;

    @Autowired
    public MergeOperationsDao(MergeOperationsMapper mapper) {
        this.mapper = mapper;
    }

    public void insert(MergeOperation operation) {
        mapper.insert(operation);
    }

    public List<MergeOperation> getPending() {
        return mapper.getPending();
    }
}
