package io.kontur.eventapi.resource;

import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.entity.SortOrder;
import io.kontur.eventapi.resource.dto.DataPaginationDTO;
import io.kontur.eventapi.resource.dto.DateTimeRange;
import io.kontur.eventapi.resource.dto.EventDto;
import io.kontur.eventapi.resource.validation.ValidBbox;
import io.kontur.eventapi.service.EventResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping("/v1")
@Validated
public class EventResource {

    private final EventResourceService eventResourceService;

    public EventResource(EventResourceService eventResourceService) {
        this.eventResourceService = eventResourceService;
    }

    @GetMapping(path = "/", produces = {APPLICATION_JSON_VALUE})
    @Operation(tags = "Events", summary = "search for events", description = "Returns events for specified feed name. All events are sorted by update date. <br> This method returns results using a cursor-based pagination approach:" +
            "<ul><li>It accepts after and limit parameters.</li>" +
            "<li>If you don't pass an after parameter the default value retrieves the first portion (or \"page\") of results.</li>" +
            "<li>Paginated responses include a top-level responseMetadata object that includes a nextAfterValue.</li>" +
            "<li>On your next call to the same method, set the after parameter equal to the nextAfterValue value you received on the last request to retrieve the next portion of the collection.</li>" +
            "<li>nextAfterValue equals to the latest updatedAt event value on the page.</li></ul>")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DataPaginationDTO.class)))
    @ApiResponse(responseCode = "204", description = "No content. Try to check filters values.", content = @Content())
    @PreAuthorize("hasAuthority('SCOPE_read:feed:'+#feed)")
    public ResponseEntity<DataPaginationDTO> searchEvents(
            @Parameter(description = "Feed name") @RequestParam(value = "feed")
                    String feed,
            @Parameter(description = "Filters events by type. More than one can be chosen at once") @RequestParam(value = "types", defaultValue = "")
                    List<EventType> eventTypes,
            @Parameter(description = "Filters events by severity. More than one can be chosen at once") @RequestParam(value = "severities", defaultValue = "")
                    List<Severity> severities,
            @Parameter(description = "Includes events that were updated after this time. `updatedAt` property is used for selection. A date-time in ISO8601 format (e.g. \\\"2020-04-12T23:20:50.52Z\\\")") @RequestParam(value = "after", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime updatedAfter,
            @Parameter(schema = @Schema(type = "string"),
                    description = "Either a date-time or an interval, open or closed. Date and time expressions adhere to RFC 3339. Open intervals are expressed using double-dots.  Examples:  * A date-time: \"2018-02-12T23:20:50Z\" * A closed interval: \"2020-01-01T00:00:00Z/2020-12-01T00:00:00Z\" * Open intervals: \"2020-01-01T00:00:00Z/..\" or \"../2020-12-01T00:00:00Z\"  Only events that have a `startedAt` - `endedAt` interval that intersects the value of `datetime` are selected.", example = "2020-01-01T00:00:00Z/2020-12-01T00:00:00Z")
            @RequestParam(value = "datetime", required = false)
                    DateTimeRange datetime,
            @Parameter(description = "Only hazards that have a geometry that intersects the bounding box are selected. The bounding box is provided as four numbers  * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2  The coordinate reference system of the values is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84). For WGS 84 longitude/latitude the values are the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude.")
            @RequestParam(value = "bbox", required = false) @ValidBbox List<BigDecimal> bbox,
            @Parameter(description = "Number of records on the page. Default value is 20, minimum - 1, maximum - 1000", example = "20", schema = @Schema(allowableValues = {}, minimum = "1", maximum = "1000")) @RequestParam(value = "limit", defaultValue = "20")
            @Min(1) @Max(1000) int limit,
            @Parameter(description = "Sort selection. Default value is ASC") @RequestParam(value = "sortOrder", defaultValue = "ASC") SortOrder sortOrder) {
        List<EventDto> events = eventResourceService.searchEvents(feed, eventTypes,
                datetime != null && datetime.getFrom() != null ? datetime.getFrom() : null,
                datetime != null && datetime.getTo() != null ? datetime.getTo() : null,
                updatedAfter, limit, severities, sortOrder, bbox);
        if (events.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new DataPaginationDTO(events, events.get(events.size() - 1).getUpdatedAt()));
    }

    @GetMapping(path = "/observations/{observationId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(tags = "Events", summary = "returns raw data", description = "Returns raw data which was used to combine events and episodes.")
    @PreAuthorize("hasAuthority('SCOPE_read:raw-data')")
    public ResponseEntity<String> rawData(@Parameter(description = "Observation UUID. May be gathered from event's 'observations' field") @PathVariable UUID observationId) {
        String rawData = eventResourceService.getRawData(observationId);
        if (StringUtils.isEmpty(rawData)) {
            return ResponseEntity.notFound().build();
        }

        if (rawData.trim().startsWith("<")) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(rawData);
        } else {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(rawData);
        }
    }

    @GetMapping(path = "/event", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(tags = "Events", summary = "returns event", description = "Returns event by its version, id and feed alias. If no version is provided the latest event version is returned.")
    @PreAuthorize("hasAuthority('SCOPE_read:feed:'+#feed)")
    public ResponseEntity<FeedData> getLastEventById(@Parameter(description = "Feed name") @RequestParam(value = "feed") String feed,
                                                     @Parameter(description = "Version") @RequestParam(value = "version", required = false) Long version,
                                                     @Parameter(description = "Event UUID") @RequestParam(value = "eventId") UUID eventId) {
        return eventResourceService.getEventByEventIdAndByVersionOrLast(eventId, feed, version)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
