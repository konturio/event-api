package io.kontur.eventapi.tornadojapanma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MainDamageSituation {
    private String dead;
    private String injuredPerson;
    private String completelyDestroyedHouse;
    private String halfDestroyedHouse;
}
