package io.kontur.eventapi.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.NonNull;

import java.util.Collection;

import static io.kontur.eventapi.util.CacheUtil.*;
import static java.lang.Thread.currentThread;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static java.util.Collections.singletonList;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .enableStatistics()
                .transactionAware()
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig(currentThread().getContextClassLoader())
                        .entryTtl(ofHours(1)))
                .build();
    }

    @Bean
    public CacheManager longCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .enableStatistics()
                .transactionAware()
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig(currentThread().getContextClassLoader())
                        .entryTtl(ofDays(14)))
                .build();
    }

    @Bean
    public CacheResolver cacheResolver(RedisConnectionFactory redisConnectionFactory) {
        return new CustomCacheResolver(cacheManager(redisConnectionFactory));
    }

    private record CustomCacheResolver(CacheManager cacheManager) implements CacheResolver {

        @Override
        @NonNull
        public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
            if (CACHED_TARGET.equals(context.getTarget().getClass().getSimpleName())
                    && EVENT_LIST_CACHED_METHOD.equals(context.getMethod().getName())) {
                return singletonList(cacheManager.getCache(EVENT_LIST_CACHE_NAME_PREFIX + context.getArgs()[0]));
            }
            throw new RuntimeException("CustomCacheResolver is used for unsupported method");
        }
    }
}
