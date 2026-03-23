package com.warp.warp_backend.integration.url;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.ConstantValue;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class DeleteUrlTest extends BaseIntegrationContextTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RedisTemplate<String, CachedUrl> redisTemplate;

  @BeforeEach
  void setUp() {
    mockAuthForUser(TestConstant.TEST_CLERK_USER_ID);
  }

  @AfterEach
  void tearDown() {
    urlRepository.deleteAll();
  }

  private Long resolveTestUserId() {
    return userRepository.findByClerkUserId(TestConstant.TEST_CLERK_USER_ID)
        .orElseThrow()
        .getId();
  }

  private Url buildUrl(Long userId, String shortCode) {
    return Url.builder()
        .id(urlRepository.getNextId())
        .userId(userId)
        .shortUrl(shortCode)
        .destinationUrl(TestConstant.DESTINATION_URL)
        .build();
  }

  @Test
  @Transactional
  void deleteUrl_success_returnsOk() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();
    Url url = urlRepository.save(buildUrl(userId, "del-url-1"));

    mockMvc.perform(withAuth(delete(ApiPath.DELETE_URL, url.getId())))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
  }

  @Test
  @Transactional
  void deleteUrl_success_setsDeletedDate() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();
    Url url = urlRepository.save(buildUrl(userId, "del-url-2"));

    mockMvc.perform(withAuth(delete(ApiPath.DELETE_URL, url.getId())))
        .andExpect(status().isOk());

    Url updated = urlRepository.findById(url.getId()).orElseThrow();
    Assertions.assertNotNull(updated.getDeletedDate());
  }
  @Test
  @Transactional
  void deleteUrl_success_redirectReturnsNotFound() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();
    Url url = urlRepository.save(buildUrl(userId, "del-url-3"));

    String cacheKey = ConstantValue.URL_CACHE_PREFIX + url.getShortUrl();
    redisTemplate.opsForValue().set(cacheKey, CachedUrl.builder()
        .urlId(url.getId())
        .shortUrl(url.getShortUrl())
        .status(UrlStatus.ACTIVE)
        .destinationUrl(url.getDestinationUrl())
        .build());
    Assertions.assertNotNull(redisTemplate.opsForValue().get(cacheKey), "Cache should be populated before delete");

    mockMvc.perform(withAuth(delete(ApiPath.DELETE_URL, url.getId())))
        .andExpect(status().isOk());

    Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey), "Cache should be evicted after delete");

    mockMvc.perform(get(ApiPath.REDIRECT, url.getShortUrl()))
        .andExpect(status().isFound())
        .andExpect(MockMvcResultMatchers.header().string("Location",
            org.hamcrest.Matchers.containsString(ApiPath.NOT_FOUND)));
  }

  @Test
  @Transactional
  void deleteUrl_notOwned_returns403() throws Exception {
    String otherClerkId = "other_clerk_user_id";
    mockAuthForUser(otherClerkId);
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long otherUserId = userRepository.findByClerkUserId(otherClerkId).orElseThrow().getId();

    Url url = urlRepository.save(buildUrl(otherUserId, "del-url-other"));

    mockAuthForUser(TestConstant.TEST_CLERK_USER_ID);
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();

    mockMvc.perform(withAuth(delete(ApiPath.DELETE_URL, url.getId())))
        .andExpect(status().isForbidden())
        .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
        .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorCode.URL_ACCESS_FORBIDDEN.getCode()));
  }

  @Test
  void deleteUrl_notFound_returns404() throws Exception {
    mockMvc.perform(withAuth(delete(ApiPath.DELETE_URL, 999999L)))
        .andExpect(status().isNotFound())
        .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
        .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorCode.DESTINATION_URL_NOT_FOUND.getCode()));
  }

  @Test
  @Transactional
  void deleteUrl_alreadyDeleted_returns404() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();
    Url url = Url.builder()
        .id(urlRepository.getNextId())
        .userId(userId)
        .shortUrl("del-url-already")
        .destinationUrl(TestConstant.DESTINATION_URL)
        .deletedDate(Instant.now())
        .build();
    urlRepository.save(url);

    mockMvc.perform(withAuth(delete(ApiPath.DELETE_URL, url.getId())))
        .andExpect(status().isNotFound())
        .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorCode.DESTINATION_URL_NOT_FOUND.getCode()));
  }

  @Test
  void deleteUrl_unauthenticated_returns401() throws Exception {
    mockMvc.perform(delete(ApiPath.DELETE_URL, 1L))
        .andExpect(status().isUnauthorized());
  }
}
