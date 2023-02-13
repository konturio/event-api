package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.*;
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
                       @Param("geomFuncType") Integer geomFuncType,
                       @Param("loss") Map<String, Object> loss,
                       @Param("active") Boolean active);

    /**
     * Mark events below specified version outdated
     */
    void markOutdatedEventsVersions(@Param("eventId") UUID eventId,
                                    @Param("feedId") UUID feedId,
                                    @Param("version") Long version);

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
