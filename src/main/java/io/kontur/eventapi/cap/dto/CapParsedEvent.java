package io.kontur.eventapi.cap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CapParsedEvent {
    private String data;

    public CapParsedEvent(String data) {
        this.data = data;
    }
}
