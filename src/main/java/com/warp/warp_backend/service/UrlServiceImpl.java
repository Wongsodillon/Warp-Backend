package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.Url;
import com.warp.warp_backend.model.exception.NotFoundException;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.repository.UrlRepository;
import com.warp.warp_backend.service.util.UrlServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class UrlServiceImpl implements UrlService {

  @Autowired
  private UrlRepository urlRepository;

  @Autowired
  private UrlServiceUtil urlServiceUtil;

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
}
