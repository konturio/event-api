package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.MergeOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface MergeOperationsMapper {
    List<MergeOperation> getPendingOperations();

    void markExecuted(@Param("operationId") UUID operationId);
}
