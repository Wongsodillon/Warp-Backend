package com.warp.warp_backend.integration.url;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.FailedTestDto;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.repository.UrlRepository;
import joptsimple.internal.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

public class ShortenUrlTest extends BaseIntegrationContextTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UrlRepository urlRepository;

  @BeforeEach
  void setUp() {
    mockAuthForUser(TestConstant.TEST_CLERK_USER_ID);
  }

  @Test
  @Transactional
  void shorten_validRequest_returns200() throws Exception {
    CreateUrlRequest request = CreateUrlRequest.builder()
        .destinationUrl(TestConstant.DESTINATION_URL)
        .build();

    String responseString =
        mockMvc.perform(withAuth(post(ApiPath.SHORTEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    RestSingleResponse<CreateUrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertTrue(response.isSuccess());
    Assertions.assertTrue(StringUtils.isNotBlank(response.getValue().getShortUrl()));
    Assertions.assertEquals(TestConstant.DESTINATION_URL, response.getValue().getDestinationUrl());
  }

  @Test
  @Transactional
  void shorten_withPassword_returns200AndIsProtected() throws Exception {
    CreateUrlRequest request = CreateUrlRequest.builder()
        .destinationUrl(TestConstant.DESTINATION_URL)
        .password(TestConstant.TEST_PASSWORD)
        .build();

    String responseString =
        mockMvc.perform(withAuth(post(ApiPath.SHORTEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    RestSingleResponse<CreateUrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertTrue(response.isSuccess());
    String shortCode = response.getValue().getShortUrl()
        .substring(response.getValue().getShortUrl().lastIndexOf('/') + 1);
    Url saved = urlRepository.findByShortUrl(shortCode).orElseThrow();
    Assertions.assertTrue(saved.isProtected());
    Assertions.assertNotNull(saved.getPassword());
    Assertions.assertNotEquals(TestConstant.TEST_PASSWORD, saved.getPassword());
  }

  @Test
  void shorten_emptyDestinationUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(Strings.EMPTY)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_BLANK)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_localhostUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.LOCALHOST_URL)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_INVALID)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_privateIpUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.PRIVATE_IP_URL)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_INVALID)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_publicRawIpUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.RAW_IP_URL)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_INVALID)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_ftpSchemeUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.FTP_URL)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_INVALID)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_malformedUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.MALFORMED_URL)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_INVALID)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_multicastUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.MULTICAST_IP_URL)
            .build())
        .errorCode(ErrorCode.DESTINATION_URL_IS_INVALID)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  @Transactional
  void shorten_withCustomShortUrl_returns200() throws Exception {
    CreateUrlRequest request = CreateUrlRequest.builder()
        .destinationUrl(TestConstant.DESTINATION_URL)
        .customShortUrl(TestConstant.CUSTOM_SHORT_URL_VALUE)
        .build();

    String responseString =
        mockMvc.perform(withAuth(post(ApiPath.SHORTEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    RestSingleResponse<CreateUrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertTrue(response.isSuccess());
    Assertions.assertTrue(response.getValue().getShortUrl().endsWith("/" + TestConstant.CUSTOM_SHORT_URL_VALUE));
    Assertions.assertEquals(TestConstant.DESTINATION_URL, response.getValue().getDestinationUrl());
  }

  @Test
  void shorten_customShortUrlTooLong_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.DESTINATION_URL)
            .customShortUrl(TestConstant.TOO_LONG_CUSTOM_SHORT_URL)
            .build())
        .errorCode(ErrorCode.CUSTOM_SHORT_URL_TOO_LONG)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_customShortUrlInvalidFormat_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.DESTINATION_URL)
            .customShortUrl(TestConstant.INVALID_FORMAT_CUSTOM_SHORT_URL)
            .build())
        .errorCode(ErrorCode.CUSTOM_SHORT_URL_INVALID_FORMAT)
        .httpStatus(HttpStatus.BAD_REQUEST)
        .build());
  }

  @Test
  void shorten_reservedCustomShortUrl_returns400() throws Exception {
    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.DESTINATION_URL)
            .customShortUrl("not-found-url")
            .build())
        .errorCode(ErrorCode.CUSTOM_SHORT_URL_ALREADY_EXISTS)
        .httpStatus(HttpStatus.CONFLICT)
        .build());
  }

  @Test
  @Transactional
  void shorten_blankCustomShortUrl_returns200AndAutoGenerates() throws Exception {
    CreateUrlRequest request = CreateUrlRequest.builder()
        .destinationUrl(TestConstant.DESTINATION_URL)
        .customShortUrl("   ")
        .build();

    String responseString =
        mockMvc.perform(withAuth(post(ApiPath.SHORTEN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    RestSingleResponse<CreateUrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertTrue(response.isSuccess());
    Assertions.assertFalse(response.getValue().getShortUrl().endsWith("/   "));
  }

  @Test
  @Transactional
  void shorten_duplicateCustomShortUrl_returns409() throws Exception {
    Url url = Url.builder()
        .id(urlRepository.getNextId())
        .destinationUrl(TestConstant.DESTINATION_URL)
        .shortUrl(TestConstant.DUPLICATE_CUSTOM_SHORT_URL)
        .build();
    urlRepository.save(url);

    runFailedTest(FailedTestDto.builder()
        .mockMvc(mockMvc)
        .httpMethod(HttpMethod.POST)
        .path(ApiPath.SHORTEN_URL)
        .body(CreateUrlRequest.builder()
            .destinationUrl(TestConstant.DESTINATION_URL)
            .customShortUrl(TestConstant.DUPLICATE_CUSTOM_SHORT_URL)
            .build())
        .errorCode(ErrorCode.CUSTOM_SHORT_URL_ALREADY_EXISTS)
        .httpStatus(HttpStatus.CONFLICT)
        .build());
  }
}
