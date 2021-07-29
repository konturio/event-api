package io.kontur.eventapi.gdacs.dto;

import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
public class ParsedAlert {
    private OffsetDateTime dateModified;
    private OffsetDateTime sent;
    private String identifier;
    private String eventId;
    private String eventType;
    private String currentEpisodeId;
    private String data;
    private String headLine;
    private String description;
    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;
    private String event;
    private String severity;

    public ParsedAlert() {
    }

    public ParsedAlert(OffsetDateTime dateModified, String identifier, String eventId, String eventType, String currentEpisodeId, String data) {
        this.dateModified = dateModified;
        this.identifier = identifier;
        this.eventId = eventId;
        this.eventType = eventType;
        this.currentEpisodeId = currentEpisodeId;
        this.data = data;
    }
}
