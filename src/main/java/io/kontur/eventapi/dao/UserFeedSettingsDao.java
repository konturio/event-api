package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.UserFeedSettingsMapper;
import io.kontur.eventapi.entity.UserFeedSettings;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserFeedSettingsDao {
    private final UserFeedSettingsMapper mapper;

    public UserFeedSettingsDao(UserFeedSettingsMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<UserFeedSettings> getUserFeedSettings(String userName) {
        return mapper.getUserFeedSettings(userName);
    }

    public void upsertUserFeedSettings(String userName, List<String> feeds, String defaultFeed) {
        mapper.upsertUserFeedSettings(userName, feeds, defaultFeed);
    }

    public void updateDefaultFeed(String userName, String defaultFeed) {
        mapper.updateDefaultFeed(userName, defaultFeed);
    }
}
