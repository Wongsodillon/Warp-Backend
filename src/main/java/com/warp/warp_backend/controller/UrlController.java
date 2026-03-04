package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlController {

  @Autowired
  private UrlService urlService;

  @GetMapping(path = ApiPath.REDIRECT)
  public ResponseEntity<Void> redirect(@PathVariable String shortUrl) {
    RedirectResponse redirectResponse = urlService.resolveDestination(shortUrl);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(redirectResponse.getLocation())
        .build();
  }
}
