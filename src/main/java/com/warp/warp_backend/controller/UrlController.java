package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.annotation.Validate;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(UrlController.class);

  @Autowired
  private UrlService urlService;

  @GetMapping(path = ApiPath.REDIRECT)
  public ResponseEntity<Void> redirect(@PathVariable String shortUrl) {
    long start = System.currentTimeMillis();
    RedirectResponse redirectResponse = urlService.resolveDestination(shortUrl);
//    log.info("[redirect   ] shortUrl={} {}ms total", shortUrl, System.currentTimeMillis() - start);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(redirectResponse.getLocation())
        .build();
  }

  @PostMapping(
      path = ApiPath.SHORTEN_URL,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public RestSingleResponse<CreateUrlResponse> shortenUrl(@RequestBody @Validate CreateUrlRequest request) {
    CreateUrlResponse createUrlResponse = urlService.shortenUrl(request);
    return this.toResponseSingleResponse(createUrlResponse);
  }
}
