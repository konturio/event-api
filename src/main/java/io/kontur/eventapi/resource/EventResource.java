package io.kontur.eventapi.resource;

import io.kontur.eventapi.resource.dto.EventDto;
import io.kontur.eventapi.service.EventResourceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController()
@RequestMapping("/v1")
public class EventResource {

    private final EventResourceService eventResourceService;

    public EventResource(EventResourceService eventResourceService) {
        this.eventResourceService = eventResourceService;
    }

    @GetMapping(path = "/", produces = {APPLICATION_JSON_VALUE})
    public List<EventDto> searchEvents(
            @Parameter(description = "Feed alias") @RequestParam(value = "feed")
                    String feed,
            @Parameter(description = "Includes hazards that were updated after this time. A date-time in ISO8601 format (e.g. \"1985-04-12T23:20:50.52Z\")") @RequestParam(value = "after", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime after,
            @Parameter(description = "Pagination offset", example = "0", schema = @Schema(allowableValues = {})) @RequestParam(value = "offset", defaultValue = "0")
                    @Min(0) int offset,
            @Parameter(description = "Number of records on the page. Default value is 20.", example = "20", schema = @Schema(allowableValues = {}, minimum = "1", maximum = "1000")) @RequestParam(value = "limit", defaultValue = "20")
                    @Min(1) @Max(1000) int limit
    ) {
        return eventResourceService.searchEvents(feed, after, offset, limit);
    }
}
