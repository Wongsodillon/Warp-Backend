package com.warp.warp_backend.service;

import com.github.benmanes.caffeine.cache.Cache;
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
  private static final Duration NOT_FOUND_TTL = Duration.ofMinutes(1);

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private CacheUtil cacheUtil;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private Cache<String, CachedUrl> urlL1Cache;

  public CachedUrl findCachedUrl(String shortUrl) {
    String key = ConstantValue.URL_CACHE_PREFIX + shortUrl;

    // ── L1 CHECK ──
    CachedUrl l1Entry = urlL1Cache.getIfPresent(key);
    if (Objects.nonNull(l1Entry)) {
      if (l1Entry.getStatus() == UrlStatus.ACTIVE
          && Objects.nonNull(l1Entry.getExpiryDate())
          && System.currentTimeMillis() >= l1Entry.getExpiryDate()) {
        // Stale ACTIVE entry — URL expired within the 5-min L1 window
        urlL1Cache.invalidate(key);
        log.debug("[L1 STALE evicted] shortUrl={}", shortUrl);
      } else {
        log.debug("[L1 HIT] shortUrl={} status={}", shortUrl, l1Entry.getStatus());
        meterRegistry.counter("url.cache.hits").increment();
        meterRegistry.counter("url.cache.hits", "layer", "l1").increment();
        return l1Entry;
      }
    }

    // ── L2 CHECK (Redis, circuit-breaker protected) ──
    CachedUrl l2Entry = cacheUtil.get(key);
    if (Objects.nonNull(l2Entry)) {
      log.debug("[L2 HIT] shortUrl={} status={}", shortUrl, l2Entry.getStatus());
      meterRegistry.counter("url.cache.hits").increment();
      meterRegistry.counter("url.cache.hits", "layer", "l2").increment();
      urlL1Cache.put(key, l2Entry);
      return l2Entry;
    }

    // ── BOTH MISS — go to DB ──
    log.debug("[cache MISS] shortUrl={}", shortUrl);
    meterRegistry.counter("url.cache.misses").increment();

    return urlRepository.findByShortUrl(shortUrl)
        .map(u -> resolveFromDb(key, u))
        .orElseGet(() -> {
          log.debug("[L1 & L2 SET NOT_FOUND] key={}", key);
          return cacheAndReturn(key, CachedUrl.builder()
              .status(UrlStatus.NOT_FOUND)
              .build(), NOT_FOUND_TTL);
        });
  }

  private CachedUrl resolveFromDb(String key, Url u) {
    if (Objects.nonNull(u.getDeletedDate()) || u.isDisabled()) {
      log.debug("[L1 & L2 SET NOT_FOUND] key={}", key);
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
            .urlId(u.getId())
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

  private CachedUrl cacheAndReturn(String key, CachedUrl entry, Duration redisTtl) {
    cacheUtil.set(key, entry, redisTtl);
    urlL1Cache.put(key, entry);
    log.debug("[cache SET {}] key={} ttl={}", entry.getStatus(), key, redisTtl);
    return entry;
  }

  public void evictUrl(String shortUrl) {
    String key = ConstantValue.URL_CACHE_PREFIX + shortUrl;
    cacheUtil.delete(key);
    urlL1Cache.invalidate(key);
    log.debug("[cache EVICT] key={}", key);
  }
}