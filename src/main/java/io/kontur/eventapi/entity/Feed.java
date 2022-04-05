package io.kontur.eventapi.entity;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class Feed {

    private UUID feedId;
    private String description;
    private String alias;
    private List<String> providers;
    private List<String> roles;
    private List<String> enrichment;
    private String enrichmentRequest;
    private List<String> enrichmentPostProcessors;
}
