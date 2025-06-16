package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.MergedGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface MergedGroupsMapper {
    UUID findGroupIdByEvent(@Param("eventId") UUID eventId);

    BigDecimal getMinPrimaryIdx(@Param("mergeGroupId") UUID mergeGroupId);

    List<MergedGroup> getGroup(@Param("mergeGroupId") UUID mergeGroupId);

    void insert(MergedGroup group);

    void updateGroupId(@Param("fromGroup") UUID fromGroup,
                       @Param("toGroup") UUID toGroup);

    void updatePrimaryIdx(@Param("mergeGroupId") UUID mergeGroupId,
                          @Param("eventId") UUID eventId,
                          @Param("idx") BigDecimal idx);
}
