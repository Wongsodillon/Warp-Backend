package com.warp.warp_backend.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlClickEvent {
  private UUID eventId;
  private Long urlId;
  private String shortUrl;
  private Instant timestamp;
  private String countryCode;
  private String deviceType;
  private String browser;
  private String referrer;
  private long responseLatencyMs;
}
