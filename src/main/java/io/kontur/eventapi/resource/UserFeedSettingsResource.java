package io.kontur.eventapi.resource;

import io.kontur.eventapi.entity.UserFeedSettings;
import io.kontur.eventapi.service.UserFeedSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1")
public class UserFeedSettingsResource {

    private final UserFeedSettingsService service;

    public UserFeedSettingsResource(UserFeedSettingsService service) {
        this.service = service;
    }

    @GetMapping(path = "/user_feed_settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = "Feeds", summary = "Get feeds settings for current user")
    public ResponseEntity<UserFeedSettings> getUserSettings(Authentication authentication) {
        String user = authentication.getName();
        Optional<UserFeedSettings> settings = service.getUserFeedSettings(user);
        return settings.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.ok(new UserFeedSettings()));
    }

    @PutMapping(path = "/user_feed_settings/default", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = "Feeds", summary = "Set default feed for current user")
    public ResponseEntity<Void> setDefaultFeed(Authentication authentication, @RequestBody Map<String, String> body) {
        String user = authentication.getName();
        service.updateDefaultFeed(user, body.get("defaultFeed"));
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/admin/user_feed_settings/{user}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = "Feeds", summary = "Get feeds settings for specified user")
    public ResponseEntity<UserFeedSettings> adminGetUserSettings(@PathVariable("user") String user) {
        return service.getUserFeedSettings(user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new UserFeedSettings()));
    }

    @PutMapping(path = "/admin/user_feed_settings/{user}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(tags = "Feeds", summary = "Update feeds available for user")
    public ResponseEntity<Void> adminSetUserFeeds(@PathVariable("user") String user, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> feeds = (List<String>) body.get("feeds");
        String defaultFeed = (String) body.get("defaultFeed");
        service.upsertUserFeedSettings(user, feeds, defaultFeed);
        return ResponseEntity.ok().build();
    }
}
