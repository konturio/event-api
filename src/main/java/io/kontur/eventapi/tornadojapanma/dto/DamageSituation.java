package io.kontur.eventapi.tornadojapanma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamageSituation {
    private String dead;
    private InjuredPerson injuredPerson;
    private Damage dwellingDamage;
    private Damage nonResidentialDamage;
    private String otherDamage;
}
