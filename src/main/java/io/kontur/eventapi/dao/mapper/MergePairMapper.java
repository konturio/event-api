package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.resource.dto.MergeCandidatePairDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MergePairMapper {

    MergeCandidatePairDTO takeNextPair();

    MergeCandidatePairDTO getPairByIds(@Param("eventId1") String eventId1,
                                       @Param("eventId2") String eventId2);

    void markAsTaken(@Param("eventId1") String eventId1,
                     @Param("eventId2") String eventId2);
}
