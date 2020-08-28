package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.Feed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface FeedMapper {

    List<Feed> getFeeds();

    int insertFeedData(
            UUID eventId, UUID feedId, Long version, String name, String description,
            OffsetDateTime startedAt, OffsetDateTime endedAt, OffsetDateTime updatedAt,
            @Param("observations") List<UUID> observations, @Param("episodes") String episodes);

    List<FeedData> searchForEvents(String feedAlias, OffsetDateTime after, int offset, int limit);

}
