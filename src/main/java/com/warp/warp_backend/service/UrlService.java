package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.exception.BaseException;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.properties.ApplicationProperties;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.util.UrlServiceUtil;
import com.warp.warp_backend.util.Base62;
import com.warp.warp_backend.util.UrlValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class UrlService {

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UrlServiceUtil urlServiceUtil;

  @Autowired
  private CurrentUserService currentUserService;

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private UrlValidationUtil urlValidationUtil;

  @Autowired
  private UrlCacheService urlCacheService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public RedirectResponse resolveDestination(String shortUrl) {
    CachedUrl cached = urlCacheService.findCachedUrl(shortUrl);

    if (cached.getStatus() == UrlStatus.NOT_FOUND) {
      return RedirectResponse.builder()
          .location(URI.create(applicationProperties.getFrontendUrl() + "/not-found"))
          .build();
    }
    if (cached.getStatus() == UrlStatus.EXPIRED) {
      return RedirectResponse.builder()
          .location(URI.create(applicationProperties.getFrontendUrl() + "/expired"))
          .build();
    }
    if (cached.isProtected()) {
      return RedirectResponse.builder()
          .location(URI.create(applicationProperties.getFrontendUrl() + "/" + cached.getShortUrl() + "/protected"))
          .build();
    }

    return RedirectResponse.builder()
        .shortUrl(shortUrl)
        .location(URI.create(cached.getDestinationUrl()))
        .urlId(cached.getUrlId())
        .build();
  }

  public CreateUrlResponse shortenUrl(CreateUrlRequest request) {
    urlValidationUtil.validateDestinationUrl(request.getDestinationUrl());

    long id = urlRepository.getNextId();
    long obfuscated = id ^ applicationProperties.getSecret();

    String shortUrl = Base62.encode(obfuscated);

    boolean isProtected = Objects.nonNull(request.getPassword()) && !request.getPassword().isBlank();

    Url url = Url.builder()
        .id(id)
        .userId(currentUserService.getCurrentUserId())
        .shortUrl(shortUrl)
        .destinationUrl(request.getDestinationUrl())
        .expiryDate(Optional.ofNullable(request.getExpiresAt())
            .map(OffsetDateTime::toInstant)
            .orElse(null))
        .isProtected(isProtected)
        .password(isProtected ? passwordEncoder.encode(request.getPassword()) : null)
        .build();

    urlRepository.save(url);

    return CreateUrlResponse.builder()
        .shortUrl(urlServiceUtil.formatUrl(shortUrl))
        .destinationUrl(request.getDestinationUrl())
        .build();
  }

  public String verifyPassword(String shortUrl, String submittedPassword) {
    Url url = urlRepository.findByShortUrl(shortUrl)
        .orElseThrow(() -> new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND));

    if (Objects.nonNull(url.getDeletedDate()) || url.isDisabled()) {
      throw new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND);
    }
    if (Objects.nonNull(url.getExpiryDate()) && Instant.now().isAfter(url.getExpiryDate())) {
      throw new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND);
    }
    if (!passwordEncoder.matches(submittedPassword, url.getPassword())) {
      throw new BaseException(ErrorCode.INVALID_PASSWORD);
    }

    return url.getDestinationUrl();
  }
}
