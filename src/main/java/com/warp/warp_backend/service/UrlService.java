package com.warp.warp_backend.service;

import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.model.response.UrlResponse;
import org.springframework.stereotype.Service;

@Service
public interface UrlService {

  RedirectResponse resolveDestination(String shortUrl);
}
