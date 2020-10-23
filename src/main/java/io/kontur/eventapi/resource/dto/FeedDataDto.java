package io.kontur.eventapi.resource.dto;

import io.kontur.eventapi.entity.FeedData;

public class FeedDataDto {
    private FeedData event;
    private Long version;
    private String alias;

    public FeedDataDto(FeedData event, Long version, String alias) {
        this.event = event;
        this.version = version;
        this.alias = alias;
    }

    public FeedData getEvent() {
        return event;
    }

    public void setEvent(FeedData event) {
        this.event = event;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
