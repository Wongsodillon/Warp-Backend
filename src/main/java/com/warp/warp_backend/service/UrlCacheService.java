package com.warp.warp_backend.service;

import com.warp.warp_backend.model.constant.ConstantValue;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.util.CacheUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Service
public class UrlCacheService {

  private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);
  private static final Duration DEFAULT_TTL   = Duration.ofHours(1);
  private static final Duration EXPIRED_TTL   = Duration.ofMinutes(5);
  private static final Duration NOT_FOUND_TTL = Duration.ofSeconds(45);

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private CacheUtil cacheUtil;

  @Autowired
  private UrlRepository urlRepository;

  public CachedUrl findCachedUrl(String shortUrl) {
    String key = ConstantValue.URL_CACHE_PREFIX + shortUrl;

    CachedUrl cached = cacheUtil.get(key);
    if (Objects.nonNull(cached)) {
      log.info("[cache HIT] shortUrl={} status={}", shortUrl, cached.getStatus());
      meterRegistry.counter("url.cache.hits").increment();
      return cached;
    }

    log.info("[cache MISS] shortUrl={}", shortUrl);
    meterRegistry.counter("url.cache.misses").increment();

    return urlRepository.findByShortUrl(shortUrl)
        .map(u -> resolveFromDb(key, u))
        .orElseGet(() -> cacheAndReturn(key, CachedUrl.builder()
            .status(UrlStatus.NOT_FOUND)
            .build(), NOT_FOUND_TTL));
  }

  private CachedUrl resolveFromDb(String key, Url u) {
    if (Objects.nonNull(u.getDeletedDate()) || u.isDisabled()) {
      return cacheAndReturn(key, CachedUrl.builder()
          .status(UrlStatus.NOT_FOUND)
          .build(), NOT_FOUND_TTL);
    }
    if (Objects.nonNull(u.getExpiryDate()) && System.currentTimeMillis() >= u.getExpiryDate().toEpochMilli()) {
      return cacheAndReturn(key, CachedUrl.builder()
          .status(UrlStatus.EXPIRED)
          .build(), EXPIRED_TTL);
    }

    Long expiryMs = Objects.nonNull(u.getExpiryDate()) ? u.getExpiryDate().toEpochMilli() : null;

    return computeActiveTtl(expiryMs)
        .map(ttl -> cacheAndReturn(key, CachedUrl.builder()
            .status(UrlStatus.ACTIVE)
            .destinationUrl(u.getDestinationUrl())
            .expiryDate(expiryMs)
            .isProtected(u.isProtected())
            .build(), ttl))
        .orElseGet(() -> cacheAndReturn(key, CachedUrl.builder()
            .status(UrlStatus.EXPIRED)
            .build(), EXPIRED_TTL));
  }

  private Optional<Duration> computeActiveTtl(Long expiryMs) {
    if (Objects.isNull(expiryMs)) {
      return Optional.of(DEFAULT_TTL);
    }
    long remainingMs = expiryMs - System.currentTimeMillis();
    if (remainingMs <= 0) {
      return Optional.empty();
    }
    Duration remaining = Duration.ofMillis(remainingMs);
    return Optional.of(remaining.compareTo(DEFAULT_TTL) < 0 ? remaining : DEFAULT_TTL);
  }

  private CachedUrl cacheAndReturn(String key, CachedUrl entry, Duration ttl) {
    cacheUtil.set(key, entry, ttl);
    log.info("[cache SET {}] key={} ttl={}", entry.getStatus(), key, ttl);
    return entry;
  }

  public void evictUrl(String shortUrl) {
    cacheUtil.delete(ConstantValue.URL_CACHE_PREFIX + shortUrl);
  }
}
