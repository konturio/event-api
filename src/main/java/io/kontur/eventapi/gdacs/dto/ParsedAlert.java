package io.kontur.eventapi.gdacs.dto;

import io.kontur.eventapi.cap.dto.CapParsedEvent;
import lombok.*;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ParsedAlert extends CapParsedEvent {
    private OffsetDateTime dateModified;
    private OffsetDateTime sent;
    private String identifier;
    private String eventId;
    private String eventType;
    private String currentEpisodeId;
    private String headLine;
    private String description;
    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;
    private String event;
    private String severity;
    private String link;
    private String eventName;
    private String country;

    public ParsedAlert(OffsetDateTime dateModified, String identifier, String eventId, String eventType, String currentEpisodeId, String data) {
        super(data);
        this.dateModified = dateModified;
        this.identifier = identifier;
        this.eventId = eventId;
        this.eventType = eventType;
        this.currentEpisodeId = currentEpisodeId;
    }
}
