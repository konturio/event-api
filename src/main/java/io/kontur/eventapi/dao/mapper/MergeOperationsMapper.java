package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.MergeOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MergeOperationsMapper {
    void insert(@Param("operation") MergeOperation operation);
    List<MergeOperation> getPending();
}
