package com.warp.warp_backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfiguration {

    public static final long MAX_SIZE          = 50_000;
    public static final long ACTIVE_EXPIRED_NS = TimeUnit.MINUTES.toNanos(5);
    public static final long NOT_FOUND_NS      = TimeUnit.SECONDS.toNanos(60);

    @Bean
    public Cache<String, CachedUrl> urlL1Cache(MeterRegistry meterRegistry) {
        Cache<String, CachedUrl> cache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfter(new Expiry<String, CachedUrl>() {

                @Override
                public long expireAfterCreate(String key, CachedUrl value, long currentTime) {
                    if (Objects.nonNull(value) && value.getStatus() == UrlStatus.NOT_FOUND) {
                        return NOT_FOUND_NS;
                    }
                    return ACTIVE_EXPIRED_NS;
                }

                @Override
                public long expireAfterUpdate(String key, CachedUrl value,
                    long currentTime, long currentDuration) {
                    return expireAfterCreate(key, value, currentTime);
                }

                @Override
                public long expireAfterRead(String key, CachedUrl value,
                    long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .recordStats()
            .build();

        CaffeineCacheMetrics.monitor(meterRegistry, cache, "url.l1.cache");
        return cache;
    }
}