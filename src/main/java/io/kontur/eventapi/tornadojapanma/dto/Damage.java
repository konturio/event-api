package io.kontur.eventapi.tornadojapanma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Damage {
    private String total;
    private String completelyDestroyed;
    private String halfDestroyed;
    private String partiallyDamaged;
}
