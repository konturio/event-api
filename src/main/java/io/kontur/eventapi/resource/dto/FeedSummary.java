package io.kontur.eventapi.resource.dto;

import lombok.Data;

@Data
public class FeedSummary {

    private final String feed;
    private final String name;
    private final String description;
}
