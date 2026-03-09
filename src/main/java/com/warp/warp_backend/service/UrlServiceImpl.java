package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.exception.BaseException;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.general.CachedUrl;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.properties.ApplicationProperties;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.util.UrlServiceUtil;
import com.warp.warp_backend.util.Base62;
import com.warp.warp_backend.util.UrlValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Objects;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class UrlServiceImpl implements UrlService {

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

  @Override
  public RedirectResponse resolveDestination(String shortUrl) {
    CachedUrl cached = urlCacheService.findCachedUrl(shortUrl);

    if (Objects.isNull(cached) || cached.isDeleted() || cached.isDisabled()) {
      throw new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND);
    }
    if (Objects.nonNull(cached.getExpiryDate()) && System.currentTimeMillis() > cached.getExpiryDate()) {
      throw new BaseException(ErrorCode.URL_EXPIRED);
    }

    return RedirectResponse.builder()
        .shortUrl(shortUrl)
        .location(URI.create(cached.getDestinationUrl()))
        .build();
  }

  @Override
  public CreateUrlResponse shortenUrl(CreateUrlRequest request) {
    urlValidationUtil.validateDestinationUrl(request.getDestinationUrl());

    long id = urlRepository.getNextId();
    long obfuscated = id ^ applicationProperties.getSecret();

    String shortUrl = Base62.encode(obfuscated);

    Url url = Url.builder()
        .id(id)
        .userId(currentUserService.getCurrentUserId())
        .shortUrl(shortUrl)
        .destinationUrl(request.getDestinationUrl())
        .expiryDate(Optional.ofNullable(request.getExpiresAt())
            .map(OffsetDateTime::toInstant)
            .orElse(null))
        .build();

    urlRepository.save(url);

    return CreateUrlResponse.builder()
        .shortUrl(urlServiceUtil.formatUrl(shortUrl))
        .destinationUrl(request.getDestinationUrl())
        .build();
  }
}
