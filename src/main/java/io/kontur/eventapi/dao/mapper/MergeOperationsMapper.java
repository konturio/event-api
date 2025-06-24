package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.MergeOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MergeOperationsMapper {
    MergeOperation getByEventIds(@Param("eventIds") List<String> eventIds);
    MergeOperation selectNext();
    void markTaken(@Param("id") Integer id);
}
