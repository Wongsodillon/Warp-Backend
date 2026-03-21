package com.warp.warp_backend.integration.url;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.FailedTestDto;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.HttpHeader;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.request.VerifyPasswordRequest;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.UrlCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

public class VerifyPasswordTest extends BaseIntegrationContextTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private UrlCacheService urlCacheService;

  @AfterEach
  void tearDown() {
    urlCacheService.evictUrl(TestConstant.PROTECTED_SHORT_URL);
  }

  @Test
  @Transactional
  void verify_correctPassword_returns302ToDestination() throws Exception {
    urlRepository.save(buildProtectedUrl());

    mockMvc.perform(post(ApiPath.VERIFY_PASSWORD, TestConstant.PROTECTED_SHORT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                VerifyPasswordRequest.builder().password(TestConstant.TEST_PASSWORD).build())))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, TestConstant.DESTINATION_URL));
  }

  @Test
  @Transactional
  void verify_wrongPassword_returns401() throws Exception {
    urlRepository.save(buildProtectedUrl());

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.VERIFY_PASSWORD)
        .pathVariables(new Object[]{TestConstant.PROTECTED_SHORT_URL})
        .body(VerifyPasswordRequest.builder().password("wrong-password").build())
        .errorCode(ErrorCode.INVALID_PASSWORD)
        .httpStatus(HttpStatus.UNAUTHORIZED)
        .useAuth(false)
        .build());
  }

  @Test
  @Transactional
  void verify_emptyPassword_returns401() throws Exception {
    urlRepository.save(buildProtectedUrl());

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.VERIFY_PASSWORD)
        .pathVariables(new Object[]{TestConstant.PROTECTED_SHORT_URL})
        .body(VerifyPasswordRequest.builder().build())
        .errorCode(ErrorCode.INVALID_PASSWORD)
        .httpStatus(HttpStatus.UNAUTHORIZED)
        .useAuth(false)
        .build());
  }

  @Test
  void verify_urlNotFound_returns404() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.VERIFY_PASSWORD)
        .pathVariables(new Object[]{"nonexistent"})
        .body(VerifyPasswordRequest.builder().password(TestConstant.TEST_PASSWORD).build())
        .errorCode(ErrorCode.DESTINATION_URL_NOT_FOUND)
        .httpStatus(HttpStatus.NOT_FOUND)
        .useAuth(false)
        .build());
  }

  private Url buildProtectedUrl() {
    return Url.builder()
        .id(urlRepository.getNextId())
        .shortUrl(TestConstant.PROTECTED_SHORT_URL)
        .destinationUrl(TestConstant.DESTINATION_URL)
        .isProtected(true)
        .password(passwordEncoder.encode(TestConstant.TEST_PASSWORD))
        .build();
  }
}
