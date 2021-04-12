package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface FeedMapper {

    List<Feed> getFeeds();

    int insertFeedData(
            UUID eventId, UUID feedId, Long version, String name, String description,
            OffsetDateTime startedAt, OffsetDateTime endedAt, OffsetDateTime updatedAt,
            @Param("observations") List<UUID> observations, @Param("episodes") String episodes);

    List<FeedData> searchForEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from, OffsetDateTime to,
                                   OffsetDateTime updatedAfter, int limit, List<Severity> severities, SortOrder sortOrder,
                                   BigDecimal xMin, BigDecimal xMax, BigDecimal yMin, BigDecimal yMax);

    Optional<FeedData> getEventByEventIdAndByVersionOrLast(UUID eventId, String feedAlias, Long version);

    Optional<Long> getLastFeedDataVersion(UUID eventId, UUID feedId);
}
