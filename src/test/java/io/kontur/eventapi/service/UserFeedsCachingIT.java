package io.kontur.eventapi.service;

import io.kontur.eventapi.dao.ApiDao;
import io.kontur.eventapi.resource.dto.FeedDto;
import io.kontur.eventapi.test.AbstractIntegrationTest;
import io.kontur.eventapi.util.CacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.Mockito.*;

public class UserFeedsCachingIT extends AbstractIntegrationTest {

    @MockBean
    private ApiDao apiDao;

    @Autowired
    private EventResourceService eventResourceService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void clearCache() {
        if (cacheManager.getCache(CacheUtil.FEED_LIST_CACHE_NAME) != null) {
            cacheManager.getCache(CacheUtil.FEED_LIST_CACHE_NAME).clear();
        }
    }

    @Test
    public void feedsReturnedFromCache() {
        FeedDto dto = new FeedDto();
        dto.setFeed("test");
        when(apiDao.getFeeds()).thenReturn(List.of(dto));

        eventResourceService.getFeeds();
        eventResourceService.getFeeds();

        verify(apiDao, times(1)).getFeeds();
    }
}
