package io.kontur.eventapi.util;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
public class CacheUtil {
    private final CacheManager cacheManager;

    public static final String EVENT_LIST_CACHE_NAME_PREFIX = "feed:";
    public static final String CACHED_TARGET = "EventResourceService";
    public static final String EVENT_LIST_CACHED_METHOD = "searchEvents";

    public CacheUtil(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictEventListCache(String feed) {
        requireNonNull(cacheManager.getCache(EVENT_LIST_CACHE_NAME_PREFIX + feed)).clear();
    }
}
