package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.annotation.Validate;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.GeoConstant;
import com.warp.warp_backend.model.constant.HttpHeader;
import com.warp.warp_backend.model.event.UrlClickEvent;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.service.GeoLocationService;
import com.warp.warp_backend.service.UrlEventPublisher;
import com.warp.warp_backend.service.UrlService;
import com.warp.warp_backend.model.general.UserAgentInfo;
import com.warp.warp_backend.util.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RestController
public class UrlController extends BaseController {

  private static final Logger log = LoggerFactory.getLogger(UrlController.class);
  private static final String PREFETCH = "prefetch";

  @Autowired
  private UrlService urlService;

  @Autowired
  private UrlEventPublisher urlEventPublisher;

  @Autowired
  private UserAgentUtil userAgentUtil;

  @Autowired
  private GeoLocationService geoLocationService;

  @GetMapping(path = ApiPath.REDIRECT)
  public ResponseEntity<Void> redirect(@PathVariable String shortUrl, HttpServletRequest request) {

    if (isPrefetchRequest(request)) {
      return ResponseEntity.noContent().build();
    }

    long start = System.currentTimeMillis();
    RedirectResponse redirectResponse = urlService.resolveDestination(shortUrl);

    long latency = System.currentTimeMillis() - start;
    UserAgentInfo uaInfo = userAgentUtil.parseUserAgent(request.getHeader(HttpHeader.USER_AGENT));
    String cfCountry = request.getHeader(HttpHeader.CF_IP_COUNTRY);
    String countryCode = (cfCountry != null && !cfCountry.isBlank() && !cfCountry.equals(GeoConstant.CF_UNKNOWN))
        ? cfCountry
        : geoLocationService.resolveCountryCode(userAgentUtil.extractClientIp(request)).orElse(null);
    UrlClickEvent event = UrlClickEvent.builder()
        .eventId(UUID.randomUUID())
        .urlId(redirectResponse.getUrlId())
        .shortUrl(shortUrl)
        .timestamp(Instant.now())
        .countryCode(countryCode)
        .referrer(userAgentUtil.parseReferrer(request.getHeader(HttpHeader.REFERER)))
        .deviceType(uaInfo.getDeviceType())
        .browser(uaInfo.getBrowser())
        .responseLatencyMs(latency)
        .build();

    urlEventPublisher.publish(event);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(redirectResponse.getLocation())
        .build();
  }

  private boolean isPrefetchRequest(HttpServletRequest request) {
    String purpose = request.getHeader(HttpHeader.PURPOSE);
    String secPurpose = request.getHeader(HttpHeader.SEC_PURPOSE);
    String xMoz = request.getHeader(HttpHeader.X_MOZ);
    return PREFETCH.equalsIgnoreCase(purpose)
        || (Objects.nonNull(secPurpose) && secPurpose.toLowerCase().startsWith(PREFETCH))
        || PREFETCH.equalsIgnoreCase(xMoz);
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
