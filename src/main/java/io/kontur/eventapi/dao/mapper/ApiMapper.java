package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.FeedDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ApiMapper {

	List<FeedDto> getFeeds();

    Optional<String> findDataByObservationId(@Param("observationId") UUID observationId);

    String searchByEmbedding(@Param("feedAlias") String feedAlias,
                             @Param("embedding") List<Double> embedding,
                             @Param("limit") int limit,
                             @Param("episodeFilterType") EpisodeFilterType episodeFilterType);

	String searchForEvents(@Param("feedAlias") String feedAlias,
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

	String searchForEventsGeoJson(@Param("feedAlias") String feedAlias,
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

	Optional<String> getEventByEventIdAndByVersionOrLast(@Param("eventId") UUID eventId,
	                                                     @Param("feedAlias") String feedAlias,
	                                                     @Param("version") Long version,
	                                                     @Param("episodeFilterType") EpisodeFilterType episodeFilterType);
}
