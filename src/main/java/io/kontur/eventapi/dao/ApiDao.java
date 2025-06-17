package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.ApiMapper;
import io.kontur.eventapi.entity.*;
import io.kontur.eventapi.resource.dto.EpisodeFilterType;
import io.kontur.eventapi.resource.dto.FeedDto;
import io.kontur.eventapi.util.CacheUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ApiDao {

	private final ApiMapper mapper;

	public ApiDao(ApiMapper mapper) {
		this.mapper = mapper;
	}

	public List<FeedDto> getFeeds() {
		return mapper.getFeeds();
	}

	public Optional<String> findDataByObservationId(UUID observationId) {
		return mapper.findDataByObservationId(observationId);
	}

	public String searchForEvents(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
	                                      OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
	                                      List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bBox,
	                                      EpisodeFilterType episodeFilterType) {
		if (bBox != null) {
			var xMin = bBox.get(0);
			var yMin = bBox.get(1);
			var xMax = bBox.get(2);
			var yMax = bBox.get(3);
			return mapper.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder,
					xMin, xMax, yMin, yMax, episodeFilterType);
		}
		return mapper.searchForEvents(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder,
				null, null, null, null, episodeFilterType);
	}

	public String searchForEventsGeoJson(String feedAlias, List<EventType> eventTypes, OffsetDateTime from,
	                                            OffsetDateTime to, OffsetDateTime updatedAfter, int limit,
	                                            List<Severity> severities, SortOrder sortOrder, List<BigDecimal> bBox,
	                                            EpisodeFilterType episodeFilterType) {
		if (bBox != null) {
			var xMin = bBox.get(0);
			var yMin = bBox.get(1);
			var xMax = bBox.get(2);
			var yMax = bBox.get(3);
			return mapper.searchForEventsGeoJson(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder,
					xMin, xMax, yMin, yMax, episodeFilterType);
		}
		return mapper.searchForEventsGeoJson(feedAlias, eventTypes, from, to, updatedAfter, limit, severities, sortOrder,
				null, null, null, null, episodeFilterType);
	}

        public Optional<String> getEventByEventIdAndByVersionOrLast(UUID eventId, String feed, Long version, EpisodeFilterType episodeFilterType) {
                return mapper.getEventByEventIdAndByVersionOrLast(eventId, feed, version, episodeFilterType);
        }

        public String getLatestEvents(int limit, List<BigDecimal> bBox) {
                if (bBox != null) {
                        var xMin = bBox.get(0);
                        var yMin = bBox.get(1);
                        var xMax = bBox.get(2);
                        var yMax = bBox.get(3);
                        return mapper.getLatestEvents(limit, xMin, xMax, yMin, yMax);
                }
                return mapper.getLatestEvents(limit, null, null, null, null);
        }
}
