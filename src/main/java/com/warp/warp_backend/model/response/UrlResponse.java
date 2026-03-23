package com.warp.warp_backend.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlResponse {

  private String shortUrl;
  private String originalUrl;
  private Instant createdAt;
  private Instant expiresAt;
  @JsonProperty("isProtected")
  private boolean isProtected;
}
