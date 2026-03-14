package com.warp.warp_backend.util;

import com.warp.warp_backend.model.general.CachedUrl;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CacheUtil {

  private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);
  private static final String CB = "redisCache";

  @Autowired
  private RedisTemplate<String, CachedUrl> redisTemplate;

  @CircuitBreaker(name = CB, fallbackMethod = "getFallback")
  public CachedUrl get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  @CircuitBreaker(name = CB, fallbackMethod = "setFallback")
  public void set(String key, CachedUrl value, Duration ttl) {
    redisTemplate.opsForValue().set(key, value, ttl);
  }

  @CircuitBreaker(name = CB, fallbackMethod = "deleteFallback")
  public void delete(String key) {
    redisTemplate.delete(key);
  }

  private CachedUrl getFallback(String key, Throwable t) {
    log.warn("[cache ERROR] get failed key={}: {}", key, t.getMessage());
    return null;
  }

  private void setFallback(String key, CachedUrl value, Duration ttl, Throwable t) {
    log.warn("[cache ERROR] set failed key={}: {}", key, t.getMessage());
  }

  private void deleteFallback(String key, Throwable t) {
    log.warn("[cache ERROR] delete failed key={}: {}", key, t.getMessage());
  }
}
