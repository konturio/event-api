package io.kontur.eventapi.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExposureGeohash {
    private String externalId;
    private String geohash;
}
