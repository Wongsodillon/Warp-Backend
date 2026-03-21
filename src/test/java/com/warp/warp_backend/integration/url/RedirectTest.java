package com.warp.warp_backend.integration.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.warp.warp_backend.config.RedirectListener;
import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.ConstantValue;
import com.warp.warp_backend.model.constant.HttpHeader;
import com.warp.warp_backend.model.constant.KafkaTopic;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.event.UrlClickEvent;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.UrlCacheService;
import com.warp.warp_backend.util.CacheUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@EmbeddedKafka(
    partitions = 1,
    topics = {KafkaTopic.URL_CLICK_EVENTS},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=",
    "spring.kafka.consumer.auto-offset-reset=latest",
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
    "spring.kafka.consumer.properties.spring.json.value.default.type=com.warp.warp_backend.model.event.UrlClickEvent"
})
public class RedirectTest extends BaseIntegrationContextTest {

  @DynamicPropertySource
  static void kafkaConsumerGroupId(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.consumer.group-id", () -> "test-group-" + UUID.randomUUID());
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UrlCacheService urlCacheService;

  @Autowired
  private CacheUtil cacheUtil;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    RedirectListener.getUrlClickEvents().clear();
  }

  @AfterEach
  void tearDown() {
    urlCacheService.evictUrl(TestConstant.SHORT_URL);
    urlCacheService.evictUrl(TestConstant.PROTECTED_SHORT_URL);
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

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL)
            .header(HttpHeader.CF_IP_COUNTRY, "US"))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, TestConstant.DESTINATION_URL));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> !RedirectListener.getUrlClickEvents().isEmpty());

    UrlClickEvent event = RedirectListener.getUrlClickEvents().get(0);
    assertThat(event.getShortUrl()).isEqualTo(TestConstant.SHORT_URL);
    assertThat(event.getEventId()).isNotNull();
    assertThat(event.getTimestamp()).isNotNull();
    Assertions.assertEquals("US", event.getCountryCode());
    assertThat(event.getResponseLatencyMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  @Transactional
  void redirect_protectedUrl_returns302ToPasswordPage() throws Exception {
    urlRepository.save(buildProtectedUrl());

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.PROTECTED_SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION,
            containsString("/" + TestConstant.PROTECTED_SHORT_URL + ApiPath.PASSWORD)));

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> !RedirectListener.getUrlClickEvents().isEmpty());

    UrlClickEvent event = RedirectListener.getUrlClickEvents().get(0);
    assertThat(event.getShortUrl()).isEqualTo(TestConstant.PROTECTED_SHORT_URL);
    assertThat(event.getEventId()).isNotNull();
    assertThat(event.getTimestamp()).isNotNull();
    assertThat(event.getResponseLatencyMs()).isGreaterThanOrEqualTo(0);
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

  @Test
  void redirect_activeCachedUrl_returns302() throws Exception {
    cacheUtil.set(ConstantValue.URL_CACHE_PREFIX + TestConstant.SHORT_URL,
        CachedUrl.builder()
            .status(UrlStatus.ACTIVE)
            .destinationUrl(TestConstant.DESTINATION_URL)
            .build(),
        Duration.ofHours(1));

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, TestConstant.DESTINATION_URL));
  }

  @Test
  void redirect_nonExistentShortUrl_returns302ToNotFound() throws Exception {
    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, containsString(ApiPath.NOT_FOUND)));
  }

  @Test
  @Transactional
  void redirect_deletedUrl_returns302ToNotFound() throws Exception {
    Url url = buildUrl();
    url.setDeletedDate(Instant.now());
    urlRepository.save(url);

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, containsString(ApiPath.NOT_FOUND)));
  }

  @Test
  @Transactional
  void redirect_disabledUrl_returns302ToNotFound() throws Exception {
    Url url = buildUrl();
    url.setDisabled(true);
    urlRepository.save(url);

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, containsString(ApiPath.NOT_FOUND)));
  }

  @Test
  void redirect_notFoundCachedMarker_returns302ToNotFound() throws Exception {
    cacheUtil.set(
        ConstantValue.URL_CACHE_PREFIX + TestConstant.SHORT_URL,
        CachedUrl.builder().status(UrlStatus.NOT_FOUND).build(),
        Duration.ofSeconds(45));

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, containsString(ApiPath.NOT_FOUND)));
  }

  @Test
  void redirect_expiredCachedMarker_returns302ToExpired() throws Exception {
    cacheUtil.set(
        ConstantValue.URL_CACHE_PREFIX + TestConstant.SHORT_URL,
        CachedUrl.builder().status(UrlStatus.EXPIRED).build(),
        Duration.ofMinutes(5));

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, containsString(ApiPath.EXPIRED)));
  }

  @Test
  @Transactional
  void redirect_expiredUrl_returns302ToExpired() throws Exception {
    Url url = buildUrl();
    url.setExpiryDate(Instant.now().minusSeconds(1));
    urlRepository.save(url);

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeader.LOCATION, containsString(ApiPath.EXPIRED)));
  }

  @Test
  @Transactional
  void redirect_noGeoHeaders_loopbackIp_countryCodeIsNull() throws Exception {
    urlRepository.save(buildUrl());

    mockMvc.perform(get(ApiPath.REDIRECT, TestConstant.SHORT_URL))
        .andExpect(status().isFound());

    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> !RedirectListener.getUrlClickEvents().isEmpty());

    UrlClickEvent event = RedirectListener.getUrlClickEvents().get(0);
    assertThat(event.getCountryCode()).isNull();
  }
}
