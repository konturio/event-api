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
import java.util.Map;

@Mapper
public interface FeedMapper {

    List<Feed> getFeeds();

    List<Feed> getFeedsByAliases(@Param("aliases") List<String> aliases);

    int insertFeedData(@Param("eventId") UUID eventId,
                       @Param("feedId") UUID feedId,
                       @Param("version") Long version,
                       @Param("name") String name,
                       @Param("properName") String properName,
                       @Param("type") EventType type,
                       @Param("severity") Severity severity,
                       @Param("description") String description,
                       @Param("startedAt") OffsetDateTime startedAt,
                       @Param("endedAt") OffsetDateTime endedAt,
                       @Param("updatedAt") OffsetDateTime updatedAt,
                       @Param("observations") Set<UUID> observations,
                       @Param("episodes") String episodes,
                       @Param("enriched") Boolean enriched,
                       @Param("urls") List<String> urls,
                       @Param("location") String location,
                       @Param("latestSeverity") Severity latestSeverity,
                       @Param("severities") List<Severity> severities,
                       @Param("geomFuncType") Integer geomFuncType,
                       @Param("loss") Map<String, Object> loss);

    /**
     * Mark events below specified version outdated
     */
    void markOutdatedEventsVersions(@Param("eventId") UUID eventId,
                                    @Param("feedId") UUID feedId,
                                    @Param("version") Long version);

    List<FeedData> searchForEvents(@Param("feedAlias") String feedAlias,
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
                                                           @Param("version") Long version,
                                                           @Param("episodeFilterType") EpisodeFilterType episodeFilterType);

    Optional<Long> getLastFeedDataVersion(@Param("eventId") UUID eventId,
                                          @Param("feedId") UUID feedId);

    List<FeedData> getNotEnrichedEventsForFeed(@Param("feedId") UUID feedId);

    List<FeedData> getEnrichmentSkippedEventsForFeed(@Param("feedId") UUID feedId);

    void addAnalytics(@Param("feedId") UUID feedId,
                      @Param("eventId") UUID eventId,
                      @Param("version") Long version,
                      @Param("eventDetails") Map<String, Object> eventDetails,
                      @Param("enriched") Boolean enriched,
                      @Param("episodes") String episodes,
                      @Param("name") String name,
                      @Param("enrichmentAttempts") Long enrichmentAttempts,
                      @Param("enrichmentSkipped") Boolean enrichmentSkipped);

    Integer getNotEnrichedEventsCount();

    Integer getEnrichmentSkippedEventsCount();

    void createFeed(@Param("feedId") UUID feedId, @Param("alias") String alias, @Param("name") String name, @Param("providers") List<String> providers);
}
