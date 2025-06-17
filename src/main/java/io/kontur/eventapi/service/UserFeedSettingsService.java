package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.UserFeedSettingsDao;
import io.kontur.eventapi.entity.UserFeedSettings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserFeedSettingsService {
    private final UserFeedSettingsDao dao;

    public UserFeedSettingsService(UserFeedSettingsDao dao) {
        this.dao = dao;
    }

    public Optional<UserFeedSettings> getUserFeedSettings(String userName) {
        return dao.getUserFeedSettings(userName);
    }

    public void upsertUserFeedSettings(String userName, List<String> feeds, String defaultFeed) {
        dao.upsertUserFeedSettings(userName, feeds, defaultFeed);
    }

    public void updateDefaultFeed(String userName, String defaultFeed) {
        dao.updateDefaultFeed(userName, defaultFeed);
    }
}
