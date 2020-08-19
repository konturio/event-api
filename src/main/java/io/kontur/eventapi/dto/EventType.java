package io.kontur.eventapi.dto;

import java.util.Arrays;

public enum EventType {
    FLOOD("flood", "FLD"),
    TSUNAMI("tsunami", "TSM"),
    STORM_SURGE("storm-surge", "STS"),
    WILDFIRE("wildfire", "WLF"),
    TORNADO("tornado", "TRD"),
    WINTER_STORM("winter-storm", "WNS"),
    OTHER("other", "OTR");

    private final String value;
    private final String abbreviation;  //TODO remove abbreviation

    EventType(String value, String abbreviation) {
        this.value = value;
        this.abbreviation = abbreviation;
    }

    public String getValue() {
        return value;
    }
    public String getAbbreviation() {
        return abbreviation;
    }

    @Override
    public String toString() {
        return value;
    }

    public static EventType fromString(String name) {
        return Arrays.stream(EventType.values())
                .filter(t -> t.value.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum constant " + name));
    }

    public static EventType fromAbbreviation(String abbreviation) {
        return Arrays.stream(EventType.values())
                .filter(t -> t.abbreviation.equals(abbreviation))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum constant " + abbreviation));
    }
}
