package com.warp.warp_backend.integration.url;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.FailedTestDto;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.ConstantValue;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.UrlCacheService;
import com.warp.warp_backend.util.CacheUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

public class RedirectTest extends BaseIntegrationContextTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UrlCacheService urlCacheService;

  @Autowired
  private CacheUtil cacheUtil;

  @AfterEach
  void tearDown() {
    urlCacheService.evictUrl(TestConstant.SHORT_URL);
  }

  private Url buildUrl() {
    return Url.builder()
        .id(urlRepository.getNextId())
        .shortUrl(TestConstant.SHORT_URL)
        .destinationUrl(TestConstant.DESTINATION_URL)
        .build();
  }

  @Test
  @Transactional
  void redirect_validShortUrl_returns302() throws Exception {
    urlRepository.save(buildUrl());

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", TestConstant.DESTINATION_URL));
  }

  @Test
  void redirect_nonExistentShortUrl_returns404() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.GET)
        .path(ApiPath.REDIRECT)
        .pathVariables(new Object[]{TestConstant.SHORT_URL})
        .errorCode(ErrorCode.DESTINATION_URL_NOT_FOUND)
        .useAuth(false)
        .build());
  }

  @Test
  @Transactional
  void redirect_deletedUrl_returns404() throws Exception {
    Url url = buildUrl();
    url.setDeletedDate(Instant.now());
    urlRepository.save(url);

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.GET)
        .path(ApiPath.REDIRECT)
        .pathVariables(new Object[]{TestConstant.SHORT_URL})
        .errorCode(ErrorCode.DESTINATION_URL_NOT_FOUND)
        .useAuth(false)
        .build());
  }

  @Test
  @Transactional
  void redirect_disabledUrl_returns404() throws Exception {
    Url url = buildUrl();
    url.setDisabled(true);
    urlRepository.save(url);

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.GET)
        .path(ApiPath.REDIRECT)
        .pathVariables(new Object[]{TestConstant.SHORT_URL})
        .errorCode(ErrorCode.DESTINATION_URL_NOT_FOUND)
        .useAuth(false)
        .build());
  }

  @Test
  void redirect_notFoundCachedMarker_returns404() throws Exception {
    cacheUtil.set(
        ConstantValue.URL_CACHE_PREFIX + TestConstant.SHORT_URL,
        CachedUrl.builder().status(UrlStatus.NOT_FOUND).build(),
        Duration.ofSeconds(45));

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.GET)
        .path(ApiPath.REDIRECT)
        .pathVariables(new Object[]{TestConstant.SHORT_URL})
        .errorCode(ErrorCode.DESTINATION_URL_NOT_FOUND)
        .useAuth(false)
        .build());
  }

  @Test
  @Transactional
  void redirect_expiredUrl_returns410() throws Exception {
    Url url = buildUrl();
    url.setExpiryDate(Instant.now().minusSeconds(1));
    urlRepository.save(url);

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.GET)
        .path(ApiPath.REDIRECT)
        .pathVariables(new Object[]{TestConstant.SHORT_URL})
        .errorCode(ErrorCode.URL_EXPIRED)
        .useAuth(false)
        .build());
  }
}
