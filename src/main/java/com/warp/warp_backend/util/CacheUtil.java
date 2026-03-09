package com.warp.warp_backend.util;

import com.warp.warp_backend.model.general.CachedUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CacheUtil {

  private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);

  @Autowired
  private RedisTemplate<String, CachedUrl> redisTemplate;

  public CachedUrl get(String key) {
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.warn("[cache ERROR] get failed key={}: {}", key, e.getMessage());
      return null;
    }
  }

  public void set(String key, CachedUrl value, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(key, value, ttl);
    } catch (Exception e) {
      log.warn("[cache ERROR] set failed key={}: {}", key, e.getMessage());
    }
  }

  public void delete(String key) {
    try {
      redisTemplate.delete(key);
    } catch (Exception e) {
      log.warn("[cache ERROR] delete failed key={}: {}", key, e.getMessage());
    }
  }
}
