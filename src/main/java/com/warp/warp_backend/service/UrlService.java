package com.warp.warp_backend.service;

import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import org.springframework.stereotype.Service;

@Service
public interface UrlService {

  RedirectResponse resolveDestination(String shortUrl);

  CreateUrlResponse shortenUrl(CreateUrlRequest request);
}
