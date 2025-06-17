package io.kontur.eventapi.resource;

import io.kontur.eventapi.entity.UserFeedSettings;
import io.kontur.eventapi.service.UserFeedSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class UserFeedSettingsResourceTest {
    private UserFeedSettingsService service = mock(UserFeedSettingsService.class);
    private UserFeedSettingsResource resource;

    @BeforeEach
    public void setUp() {
        resource = new UserFeedSettingsResource(service);
    }

    @Test
    public void adminSetUserFeedsTest() {
        resource.adminSetUserFeeds("alice", Map.of("feeds", List.of("f1", "f2"), "defaultFeed", "f1"));
        verify(service, times(1)).upsertUserFeedSettings("alice", List.of("f1", "f2"), "f1");
    }

    @Test
    public void adminGetUserSettingsTest() {
        UserFeedSettings settings = new UserFeedSettings();
        settings.setUserName("bob");
        settings.setFeeds(List.of("feed"));
        when(service.getUserFeedSettings("bob")).thenReturn(Optional.of(settings));
        UserFeedSettings result = resource.adminGetUserSettings("bob").getBody();
        assertEquals("bob", result.getUserName());
        assertEquals(1, result.getFeeds().size());
    }
}
