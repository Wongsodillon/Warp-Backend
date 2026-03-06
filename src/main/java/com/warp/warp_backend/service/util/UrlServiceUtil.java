package com.warp.warp_backend.service.util;

import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Component
public class UrlServiceUtil {

  @Autowired
  private ApplicationProperties properties;

  public URI resolveRedirectTarget(Url url) {
    if (url.isDisabled()) {
      return URI.create("/disabled");
    }
    if (isExpired(url)) {
      return URI.create("/expired");
    }
    if (url.isProtected() && Objects.nonNull(url.getPassword())) {
      return URI.create("/password/" + url.getShortUrl());
    }
    return URI.create(url.getDestinationUrl());
  }

  public boolean isExpired(Url url) {
    Instant expiryDate = url.getExpiryDate();

    if (Objects.isNull(expiryDate)) {
      return false;
    }
    return Instant.now().isAfter(expiryDate);
  }

  public String formatUrl(String shortUrl) {
    return String.format("%s/%s", properties.getDomainUrl(), shortUrl);
  }
}
