package com.warp.warp_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warp.warp_backend.model.FailedTestDto;
import com.warp.warp_backend.model.TestConstant;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.TestInstance;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationContextTest {

  @MockitoBean
  protected JwtDecoder jwtDecoder;

  @Autowired
  private ObjectMapper objectMapper;

  protected void mockAuthForUser(String clerkUserId) {
    Jwt jwt = Jwt.withTokenValue(TestConstant.TEST_TOKEN)
        .header("alg", "RS256")
        .claim("sub", clerkUserId)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
    BDDMockito.given(jwtDecoder.decode(TestConstant.TEST_TOKEN)).willReturn(jwt);
  }

  protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request) {
    return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestConstant.TEST_TOKEN);
  }

  protected void runFailedTest(FailedTestDto failedTestDto) throws Exception {
    Objects.requireNonNull(failedTestDto.getMockMvc(), "FailedTestDto.mockMvc must not be null");
    Objects.requireNonNull(failedTestDto.getHttpMethod(), "FailedTestDto.httpMethod must not be null");
    Objects.requireNonNull(failedTestDto.getPath(), "FailedTestDto.path must not be null");

    Object[] pathVars = failedTestDto.getPathVariables() != null ? failedTestDto.getPathVariables() : new Object[0];
    MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(
        failedTestDto.getHttpMethod(), failedTestDto.getPath(), pathVars);

    if (Objects.nonNull(failedTestDto.getBody())) {
      request.contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(failedTestDto.getBody()));
    }

    if (MapUtils.isNotEmpty(failedTestDto.getAdditionalParams())) {
      request.params(failedTestDto.getAdditionalParams());
    }

    MockHttpServletRequestBuilder authorizedRequest = failedTestDto.isUseAuth() ? withAuth(request) : request;

    failedTestDto.getMockMvc()
        .perform(authorizedRequest)
        .andExpect(MockMvcResultMatchers.status().is(
            Optional.ofNullable(failedTestDto.getHttpStatus())
                .map(HttpStatus::value)
                .orElseGet(() -> failedTestDto.getErrorCode().getHttpStatus())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
        .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(
            Optional.ofNullable(failedTestDto.getCustomErrorCode())
                .orElseGet(() -> failedTestDto.getErrorCode().getCode())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage").value(
            Optional.ofNullable(failedTestDto.getCustomErrorMessage())
                .orElseGet(() -> failedTestDto.getErrorCode().getDescription())));
  }
}
