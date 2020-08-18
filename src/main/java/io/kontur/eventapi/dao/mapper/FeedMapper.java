package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.FeedDataDto;
import io.kontur.eventapi.dto.FeedDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface FeedMapper {

    List<FeedDto> getFeeds();

    int insertFeedData(
            UUID eventId, UUID feedId, Long version, String name, String description,
            OffsetDateTime startedAt, OffsetDateTime endedAt, OffsetDateTime updatedAt,
            @Param("observations") String observations, @Param("episodes") String episodes);

    List<FeedDataDto> searchForEvents(UUID feedId, OffsetDateTime after, int offset, int limit);

}
