package io.kontur.eventapi.resource;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.dao.FeedDao;
import io.kontur.eventapi.entity.Feed;
import io.kontur.eventapi.resource.dto.FeedSummary;
import io.kontur.eventapi.service.EventResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventResourceTest {
    private static final String FIRST_ALIAS = "first_alias";
    private static final String FIRST_DESCRIPTION = "first description";
    private static final String SECOND_ALIAS = "second_alias";
    private static final String SECOND_DESCRIPTION = "second description";

    FeedDao feedDao = mock(FeedDao.class);
    DataLakeDao dataLakeDao = mock(DataLakeDao.class);

    EventResourceService eventResourceService = new EventResourceService(feedDao, dataLakeDao);
    EventResource eventResource;

    @BeforeEach
    public void before() {
        eventResource = new EventResource(eventResourceService);
    }

    @Test
    public void userFeedNoRolesTest() {
        when(feedDao.getFeeds()).thenReturn(twoFeeds());
        mockAuthWithRoles(List.of("something random"));

        List<FeedSummary> feeds = eventResource.getUserFeeds().getBody();
        assertTrue(feeds.isEmpty());
    }

    @Test
    public void userFeedOneRoleTest() {
        when(feedDao.getFeeds()).thenReturn(twoFeeds());
        mockAuthWithRoles(List.of("read:feed:" + FIRST_ALIAS));

        List<FeedSummary> feeds = eventResource.getUserFeeds().getBody();

        assertEquals(1, feeds.size());
        assertEquals("first_alias", feeds.get(0).getFeed());
        assertEquals("first description", feeds.get(0).getDescription());
    }

    @Test
    public void userFeedBothRolesTest() {
        when(feedDao.getFeeds()).thenReturn(twoFeeds());
        mockAuthWithRoles(List.of("read:feed:" + FIRST_ALIAS, "read:feed:" + SECOND_ALIAS));

        List<FeedSummary> feeds = eventResource.getUserFeeds().getBody();

        assertEquals(2, feeds.size());

        Optional<FeedSummary> first = eventResource.getUserFeeds().getBody()
                .stream().filter(it -> FIRST_ALIAS.equals(it.getFeed()))
                .findAny();
        assertTrue(first.isPresent());
        assertEquals(FIRST_DESCRIPTION, first.get().getDescription());

        Optional<FeedSummary> second = eventResource.getUserFeeds().getBody()
                .stream().filter(it -> SECOND_ALIAS.equals(it.getFeed()))
                .findAny();
        assertTrue(second.isPresent());
        assertEquals(SECOND_DESCRIPTION, second.get().getDescription());

    }

    private void mockAuthWithRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        Collection<GrantedAuthority> authorities = roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        AbstractAuthenticationToken authentication = mock(AbstractAuthenticationToken.class);
        when(authentication.getAuthorities()).thenReturn(authorities);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private List<Feed> twoFeeds() {
        Feed first = new Feed();
        first.setAlias(FIRST_ALIAS);
        first.setDescription(FIRST_DESCRIPTION);
        Feed second = new Feed();
        second.setAlias(SECOND_ALIAS);
        second.setDescription(SECOND_DESCRIPTION);
        return List.of(first, second);
    }
}
