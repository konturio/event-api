package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.entity.OpenFeedData;
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

    int insertFeedData(@Param("eventId") UUID eventId,
                       @Param("feedId") UUID feedId,
                       @Param("version") Long version,
                       @Param("name") String name,
                       @Param("properName") String properName,
                       @Param("description") String description,
                       @Param("startedAt") OffsetDateTime startedAt,
                       @Param("endedAt") OffsetDateTime endedAt,
                       @Param("updatedAt") OffsetDateTime updatedAt,
                       @Param("observations") Set<UUID> observations,
                       @Param("episodes") String episodes,
                       @Param("enriched") Boolean enriched,
                       @Param("urls") List<String> urls,
                       @Param("location") String location);

    /**
     * Mark events below specified version outdated
     */
    void markOutdatedEventsVersions(@Param("eventId") UUID eventId,
                                    @Param("feedId") UUID feedId,
                                    @Param("version") Long version);

    List<OpenFeedData> searchForEvents(@Param("feedAlias") String feedAlias,
                                       @Param("eventTypes") List<EventType> eventTypes,
                                       @Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to,
                                       @Param("updatedAfter") OffsetDateTime updatedAfter,
                                       @Param("limit") int limit,
                                       @Param("severities") List<Severity> severities,
                                       @Param("sortOrder") SortOrder sortOrder,
                                       @Param("xMin") BigDecimal xMin,
                                       @Param("xMax") BigDecimal xMax,
                                       @Param("yMin") BigDecimal yMin,
                                       @Param("yMax") BigDecimal yMax,
                                       @Param("episodeFilterType") EpisodeFilterType episodeFilterType);

    Optional<FeedData> getEventByEventIdAndByVersionOrLast(@Param("eventId") UUID eventId,
                                                           @Param("feedAlias") String feedAlias,
                                                           @Param("version") Long version);

    Optional<Long> getLastFeedDataVersion(@Param("eventId") UUID eventId,
                                          @Param("feedId") UUID feedId);

    List<FeedData> getNotEnrichedEventsForFeed(@Param("feedId") UUID feedId);

    List<FeedData> getEnrichmentSkippedEventsForFeed(@Param("feedId") UUID feedId);

    void addAnalytics(@Param("feedId") UUID feedId,
                      @Param("eventId") UUID eventId,
                      @Param("version") Long version,
                      @Param("eventDetails") String eventDetails,
                      @Param("enriched") Boolean enriched,
                      @Param("episodes") String episodes,
                      @Param("name") String name,
                      @Param("enrichmentAttempts") Long enrichmentAttempts,
                      @Param("enrichmentSkipped") Boolean enrichmentSkipped);

    Integer getNotEnrichedEventsCount();

    Integer getEnrichmentSkippedEventsCount();
}
