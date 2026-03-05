package com.warp.warp_backend.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("application")
public class ApplicationProperties {

  @Value("${application.short-url.secret}")
  private long secret;
}
