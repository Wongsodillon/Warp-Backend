package com.warp.warp_backend.integration.url;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.response.RestListContentResponse;
import com.warp.warp_backend.model.response.UrlResponse;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public class ListUrlsTest extends BaseIntegrationContextTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UserRepository userRepository;

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
  void listUrls_noUrls_returnsEmptyPage() throws Exception {
    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .contentType(MediaType.APPLICATION_JSON)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertTrue(response.isSuccess());
    Assertions.assertTrue(response.getContent().isEmpty());
    Assertions.assertEquals(0, response.getTotalElements());
  }

  @Test
  @Transactional
  void listUrls_withUrls_returnsPagedResults() throws Exception {
    // Trigger user creation
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();

    urlRepository.save(buildUrl(userId, "test-url-1"));
    urlRepository.save(buildUrl(userId, "test-url-2"));

    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .contentType(MediaType.APPLICATION_JSON)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertTrue(response.isSuccess());
    Assertions.assertEquals(2, response.getTotalElements());
    Assertions.assertEquals(2, response.getContent().size());

    UrlResponse first = response.getContent().get(0);
    Assertions.assertNotNull(first.getShortUrl());
    Assertions.assertNotNull(first.getOriginalUrl());
    Assertions.assertNotNull(first.getCreatedAt());
  }

  @Test
  @Transactional
  void listUrls_activeFilter_excludesExpiredUrls() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();

    Url active = buildUrl(userId, "active-url");
    Url expired = Url.builder()
        .id(urlRepository.getNextId())
        .userId(userId)
        .shortUrl("expired-url")
        .destinationUrl(TestConstant.DESTINATION_URL)
        .expiryDate(Instant.now().minusSeconds(3600))
        .build();
    urlRepository.save(active);
    urlRepository.save(expired);

    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .param("active", "true")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertEquals(1, response.getTotalElements());
    Assertions.assertNull(response.getContent().get(0).getExpiresAt());
  }

  @Test
  @Transactional
  void listUrls_expiredFilter_returnsOnlyExpiredUrls() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();

    Url active = buildUrl(userId, "active-url-2");
    Url expired = Url.builder()
        .id(urlRepository.getNextId())
        .userId(userId)
        .shortUrl("expired-url-2")
        .destinationUrl(TestConstant.DESTINATION_URL)
        .expiryDate(Instant.now().minusSeconds(3600))
        .build();
    urlRepository.save(active);
    urlRepository.save(expired);

    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .param("active", "false")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertEquals(1, response.getTotalElements());
    Assertions.assertNotNull(response.getContent().get(0).getExpiresAt());
  }

  @Test
  @Transactional
  void listUrls_protectedFilter_returnsOnlyProtected() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();

    Url unprotected = buildUrl(userId, "unprotected-url");
    Url protectedUrl = Url.builder()
        .id(urlRepository.getNextId())
        .userId(userId)
        .shortUrl("protected-url")
        .destinationUrl(TestConstant.DESTINATION_URL)
        .isProtected(true)
        .password("hashed-password")
        .build();
    urlRepository.save(unprotected);
    urlRepository.save(protectedUrl);

    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .param("isProtected", "true")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertEquals(1, response.getTotalElements());
    Assertions.assertTrue(response.getContent().get(0).isProtected());
  }

  @Test
  @Transactional
  void listUrls_softDeletedUrls_notReturned() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();

    Url deleted = Url.builder()
        .id(urlRepository.getNextId())
        .userId(userId)
        .shortUrl("deleted-url")
        .destinationUrl(TestConstant.DESTINATION_URL)
        .deletedDate(Instant.now())
        .build();
    urlRepository.save(deleted);

    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .contentType(MediaType.APPLICATION_JSON)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertEquals(0, response.getTotalElements());
    Assertions.assertTrue(response.getContent().isEmpty());
  }

  @Test
  @Transactional
  void listUrls_pagination_returnsCorrectPage() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS))).andReturn();
    Long userId = resolveTestUserId();

    urlRepository.save(buildUrl(userId, "page-url-1"));
    urlRepository.save(buildUrl(userId, "page-url-2"));
    urlRepository.save(buildUrl(userId, "page-url-3"));

    String responseString = mockMvc.perform(withAuth(get(ApiPath.LIST_USER_URLS)
            .param("page", "1")
            .param("size", "2")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    RestListContentResponse<UrlResponse> response =
        objectMapper.readValue(responseString, new TypeReference<>() {});

    Assertions.assertEquals(3, response.getTotalElements());
    Assertions.assertEquals(2, response.getTotalPages());
    Assertions.assertEquals(1, response.getPage());
    Assertions.assertEquals(1, response.getContent().size());
  }

  @Test
  void listUrls_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get(ApiPath.LIST_USER_URLS)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
