package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.exception.BaseException;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.general.UrlStatus;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.model.response.UrlResponse;
import com.warp.warp_backend.properties.ApplicationProperties;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.util.UrlServiceUtil;
import com.warp.warp_backend.util.Base62;
import com.warp.warp_backend.util.UrlValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UrlService {

  private static final Pattern SAFE_SHORT_URL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

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
          .location(URI.create(applicationProperties.getFrontendUrl() + ApiPath.NOT_FOUND))
          .build();
    }
    if (cached.getStatus() == UrlStatus.EXPIRED) {
      return RedirectResponse.builder()
          .location(URI.create(applicationProperties.getFrontendUrl() + ApiPath.EXPIRED))
          .build();
    }
    if (cached.isProtected()) {
      return RedirectResponse.builder()
          .shortUrl(shortUrl)
          .urlId(cached.getUrlId())
          .location(URI.create(applicationProperties.getFrontendUrl() + "/" + cached.getShortUrl() + ApiPath.PASSWORD))
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

    if (Objects.nonNull(request.getExpiresAt()) && !request.getExpiresAt().toInstant().isAfter(Instant.now())) {
      throw new BaseException(ErrorCode.EXPIRES_AT_IN_THE_PAST);
    }

    String shortUrl;
    if (Objects.nonNull(request.getCustomShortUrl()) && !request.getCustomShortUrl().isBlank()) {
      if (!SAFE_SHORT_URL_PATTERN.matcher(request.getCustomShortUrl()).matches()) {
        throw new BaseException(ErrorCode.CUSTOM_SHORT_URL_INVALID_FORMAT);
      }
      if (applicationProperties.getReservedShortUrl().contains(request.getCustomShortUrl())) {
        throw new BaseException(ErrorCode.CUSTOM_SHORT_URL_ALREADY_EXISTS);
      }
      shortUrl = request.getCustomShortUrl();
      try {
        urlRepository.saveAndFlush(buildUrlEntity(urlRepository.getNextId(), shortUrl, request));
      } catch (DataIntegrityViolationException e) {
        throw new BaseException(ErrorCode.CUSTOM_SHORT_URL_ALREADY_EXISTS);
      }
    } else {
      long id = urlRepository.getNextId();
      shortUrl = Base62.encode(id ^ applicationProperties.getSecret());
      urlRepository.save(buildUrlEntity(id, shortUrl, request));
    }

    return CreateUrlResponse.builder()
        .shortUrl(urlServiceUtil.formatUrl(shortUrl))
        .destinationUrl(request.getDestinationUrl())
        .build();
  }

  private Url buildUrlEntity(long id, String shortUrl, CreateUrlRequest request) {
    boolean isProtected = Objects.nonNull(request.getPassword()) && !request.getPassword().isBlank();
    return Url.builder()
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
  }

  public List<UrlResponse> getUserUrls(int page, int size, String sortBy, String sortDir,
      Boolean active, Boolean isProtected) {
    Long userId = currentUserService.getCurrentUserId();
    List<Url> urls = urlRepository.findUserUrls(userId, active, isProtected, page, size, sortBy, sortDir);
    return urls.stream().map(this::toUrlResponse).collect(Collectors.toList());
  }

  public long countUserUrls(Boolean active, Boolean isProtected) {
    Long userId = currentUserService.getCurrentUserId();
    return urlRepository.countUserUrls(userId, active, isProtected);
  }

  private UrlResponse toUrlResponse(Url url) {
    return UrlResponse.builder()
        .shortUrl(urlServiceUtil.formatUrl(url.getShortUrl()))
        .originalUrl(url.getDestinationUrl())
        .createdAt(url.getCreatedDate())
        .expiresAt(url.getExpiryDate())
        .isProtected(url.isProtected())
        .build();
  }

  public String verifyPassword(String shortUrl, String submittedPassword) {
    Url url = urlRepository.findByShortUrl(shortUrl)
        .orElseThrow(() -> new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND));
    if (Objects.isNull(submittedPassword) || submittedPassword.isBlank()) {
      throw new BaseException(ErrorCode.INVALID_PASSWORD);
    }
    if (Objects.nonNull(url.getDeletedDate()) || url.isDisabled()) {
      throw new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND);
    }
    if (Objects.nonNull(url.getExpiryDate()) && !Instant.now().isBefore(url.getExpiryDate())) {
      throw new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND);
    }
    if (!passwordEncoder.matches(submittedPassword, url.getPassword())) {
      throw new BaseException(ErrorCode.INVALID_PASSWORD);
    }

    return url.getDestinationUrl();
  }
}
