package com.warp.warp_backend.service;

import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
public class UrlCacheService {

    private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);
    private static final String CACHE_PREFIX = "urls:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    @Autowired
    private RedisTemplate<String, CachedUrl> redisTemplate;

    @Autowired
    private UrlRepository urlRepository;

    public CachedUrl findCachedUrl(String shortUrl) {
        String key = CACHE_PREFIX + shortUrl;

        CachedUrl cached = redisTemplate.opsForValue().get(key);
        if (Objects.nonNull(cached)) {
            log.info("[cache HIT] shortUrl={}", shortUrl);
            return cached;
        }

        CachedUrl url = urlRepository.findByShortUrl(shortUrl)
            .map(u -> CachedUrl.builder()
                .destinationUrl(u.getDestinationUrl())
                .expiryDate(Objects.nonNull(u.getExpiryDate()) ? u.getExpiryDate().toEpochMilli() : null)
                .isProtected(u.isProtected())
                .disabled(u.isDisabled())
                .deleted(Objects.nonNull(u.getDeletedDate()))
                .build())
            .orElse(null);

        if (Objects.nonNull(url) && !url.isDeleted() && !url.isDisabled()) {
            redisTemplate.opsForValue().set(key, url, DEFAULT_TTL);
        }

        log.info("[cache MISS] shortUrl={}", shortUrl);
        return url;
    }

    public void evictUrl(String shortUrl) {
        redisTemplate.delete(CACHE_PREFIX + shortUrl);
    }
}
