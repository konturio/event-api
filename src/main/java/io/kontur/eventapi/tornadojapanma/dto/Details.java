package io.kontur.eventapi.tornadojapanma.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Details {
    private String type;
    private String occurrenceDateTime;
    private Place occurrencePlace;
    private String disappearanceDateTime;
    private Place disappearancePlace;
    private String fScale;
    private String damageWidth;
    private String damagedLength;
    private List<String> movementDirection;
    private String movementSpeed;
    private String duration;
    private String rotationDirection;
    private String originDifferentiation;
    private List<String> viewingArea;
    private String positionFromTheOverallScaleDisturbance;
    private DamageSituation damageSituation;
    private String features;
}
