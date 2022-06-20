package io.kontur.eventapi.cap.dto;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CapParsedItem extends CapParsedEvent {
    private String guid;
    private OffsetDateTime pubDate;
    private Double longitude;
    private Double latitude;
    private String title;
    private String description;
    private String link;
}
