package io.kontur.eventapi.util;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

import io.kontur.eventapi.resource.dto.GeometryFilterType;
import static io.kontur.eventapi.resource.dto.EpisodeFilterType.*;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Component
public class CacheUtil {
    private final CacheManager cacheManager;
    private final CacheManager longCacheManager;

    public static final String EVENT_LIST_CACHE_NAME_PREFIX = "feed:";
    public static final String EVENT_CACHE_NAME = "events";
    public static final String CACHED_TARGET = "EventResourceService";
    public static final String EVENT_LIST_CACHED_METHOD = "searchEvents";

    private static String EVENT_CACHE_KEY_FORMAT = "SimpleKey [%s,%s,null,%s,%s]";

    public CacheUtil(CacheManager cacheManager, CacheManager longCacheManager) {
        this.cacheManager = cacheManager;
        this.longCacheManager = longCacheManager;
    }

    public void evictEventListCache(String feed) {
        requireNonNull(cacheManager.getCache(EVENT_LIST_CACHE_NAME_PREFIX + feed)).clear();
    }

    public void evictEventCache(UUID eventId, String feed) {
        for (var geo : new GeometryFilterType[]{GeometryFilterType.ANY, GeometryFilterType.NONE}) {
            requireNonNull(longCacheManager.getCache(EVENT_CACHE_NAME)).evict(format(EVENT_CACHE_KEY_FORMAT, eventId, feed, ANY, geo));
            requireNonNull(longCacheManager.getCache(EVENT_CACHE_NAME)).evict(format(EVENT_CACHE_KEY_FORMAT, eventId, feed, NONE, geo));
            requireNonNull(longCacheManager.getCache(EVENT_CACHE_NAME)).evict(format(EVENT_CACHE_KEY_FORMAT, eventId, feed, LATEST, geo));
            requireNonNull(longCacheManager.getCache(EVENT_CACHE_NAME)).evict(format(EVENT_CACHE_KEY_FORMAT, eventId, feed, null, geo));
        }
    }
}
