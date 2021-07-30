package io.kontur.eventapi.entity;

import java.util.List;
import java.util.UUID;

public class Feed {

    private UUID feedId;
    private String description;
    private String alias;
    private List<String> providers;
    private List<String> roles;
    private List<String> enrichment;

    public UUID getFeedId() {
        return feedId;
    }

    public void setFeedId(UUID feedId) {
        this.feedId = feedId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getEnrichment() {
        return enrichment;
    }

    public void setEnrichment(List<String> enrichment) {
        this.enrichment = enrichment;
    }
}
