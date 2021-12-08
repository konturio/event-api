package io.kontur.eventapi.inciweb.dto;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParsedItem {
    private String guid;
    private OffsetDateTime pubDate;
    private Double longitude;
    private Double latitude;
    private String title;
    private String description;
    private String link;
    private String data;
}
