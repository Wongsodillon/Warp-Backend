package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.properties.ApplicationProperties;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.util.UrlServiceUtil;
import com.warp.warp_backend.util.Base62;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class UrlServiceImpl implements UrlService {

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UrlServiceUtil urlServiceUtil;

  @Autowired
  private ApplicationProperties applicationProperties;

  @Override
  public RedirectResponse resolveDestination(String shortUrl) {
    Url url = urlRepository.findByShortUrl(shortUrl)
        .orElseThrow(() -> new NotFoundException(ErrorCode.DESTINATION_URL_NOT_FOUND));
    URI redirectTarget = urlServiceUtil.resolveRedirectTarget(url);
    return RedirectResponse.builder()
        .shortUrl(shortUrl)
        .location(redirectTarget)
        .build();
  }

  @Override
  public CreateUrlResponse shortenUrl(CreateUrlRequest request) {

    long id = urlRepository.getNextId();
    long obfuscated = id ^ applicationProperties.getSecret();

    String shortUrl = Base62.encode(obfuscated);

    Url url = Url.builder()
        .id(id)
        .shortUrl(shortUrl)
        .destinationUrl(request.getDestinationUrl())
        .expiryDate(Optional.ofNullable(request.getExpiresAt())
            .map(OffsetDateTime::toInstant)
            .orElse(null))
        .build();

    urlRepository.save(url);

    return CreateUrlResponse.builder()
        .shortUrl(shortUrl)
        .destinationUrl(request.getDestinationUrl())
        .build();
  }
}
