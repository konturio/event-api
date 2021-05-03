package io.kontur.eventapi.tornadojapanma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedCase {
    private String type;
    private String occurrenceDateTime;
    private String occurrencePlace;
    private Details details;
    private String fScale;
    private JefScale jefScale;
    private String damageWidth;
    private String damageLength;
    private MainDamageSituation mainDamageSituation;
    private String viewingArea;
    private String remarks;
}
