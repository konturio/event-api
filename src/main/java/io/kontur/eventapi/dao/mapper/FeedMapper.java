package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface FeedMapper {

    List<Feed> getFeeds();

    List<Feed> getFeedsByAliases(@Param("aliases") List<String> aliases);

    int insertFeedData(
            UUID eventId, UUID feedId, Long version, String name, String description, OffsetDateTime startedAt,
            OffsetDateTime endedAt, OffsetDateTime updatedAt, @Param("observations") Set<UUID> observations,
            @Param("episodes") String episodes, Boolean enriched);

    /**
     * Mark events below specified version outdated
     */
    void markOutdatedEventsVersions(UUID eventId, UUID feedId, Long version);

    List<FeedData> searchForEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from, OffsetDateTime to,
                                   OffsetDateTime updatedAfter, int limit, List<Severity> severities, SortOrder sortOrder,
                                   BigDecimal xMin, BigDecimal xMax, BigDecimal yMin, BigDecimal yMax,
                                   EpisodeFilterType episodeFilterType);

    Optional<FeedData> getEventByEventIdAndByVersionOrLast(UUID eventId, String feedAlias, @Param("version") Long version);

    Optional<Long> getLastFeedDataVersion(UUID eventId, UUID feedId);

    List<FeedData> getNotEnrichedEventsForFeed(UUID feedId);

    void addAnalytics(UUID feedId, UUID eventId, Long version,
                      @Param("eventDetails") String eventDetails, Boolean enriched, @Param("episodes") String episodes);
}
