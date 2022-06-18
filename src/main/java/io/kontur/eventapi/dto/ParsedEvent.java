package io.kontur.eventapi.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParsedEvent {
    private String data;

    public ParsedEvent(String data) {
        this.data = data;
    }
}
