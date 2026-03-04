package com.warp.warp_backend.service.util;

import com.warp.warp_backend.model.entity.Url;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Component
public class UrlServiceUtil {

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
}
