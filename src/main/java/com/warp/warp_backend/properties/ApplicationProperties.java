package com.warp.warp_backend.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("application")
public class ApplicationProperties {

  @Value("${application.short-url.secret}")
  private long secret;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwksUri;

  @Value("${application.jwt-issuer}")
  private String jwtIssuer;

  @Value("${application.domain.url}")
  private String domainUrl;

  @Value("${application.frontend.url}")
  private String frontendUrl;

  @Value("#{'${application.allowed-origins}'.split(',')}")
  private List<String> allowedOrigins;

  @Value("#{'${application.reserved.short-url}'.split(',')}")
  private List<String> reservedShortUrl;

  @Value("${application.analytics.top-urls.max-limit}")
  private int topUrlsMaxLimit;
}
