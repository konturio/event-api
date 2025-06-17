package io.kontur.eventapi.entity;

import lombok.Data;

import java.util.List;

@Data
public class UserFeedSettings {
    private String userName;
    private List<String> feeds;
    private String defaultFeed;
}
