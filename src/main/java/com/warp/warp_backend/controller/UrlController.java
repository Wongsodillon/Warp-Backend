package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.annotation.Validate;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.GeoConstant;
import com.warp.warp_backend.model.constant.HttpHeader;
import com.warp.warp_backend.model.event.UrlClickEvent;
import com.warp.warp_backend.model.request.CreateUrlRequest;
import com.warp.warp_backend.model.request.VerifyPasswordRequest;
import com.warp.warp_backend.model.response.CreateUrlResponse;
import com.warp.warp_backend.model.response.RedirectResponse;
import com.warp.warp_backend.model.response.RestListContentResponse;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.model.response.UrlResponse;
import com.warp.warp_backend.model.response.VerifyPasswordResponse;
import com.warp.warp_backend.service.GeoLocationService;
import com.warp.warp_backend.service.UrlEventPublisher;
import com.warp.warp_backend.service.UrlService;
import com.warp.warp_backend.model.general.UserAgentInfo;
import com.warp.warp_backend.util.UserAgentUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "URLs", description = "URL shortening, redirect, and password verification")
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

  @Operation(summary = "Redirect to destination URL", description = "Resolves a short URL and redirects the client. Publishes a click analytics event. Returns 204 for browser prefetch requests.")
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "Redirect to destination URL"),
      @ApiResponse(responseCode = "302", description = "Password-protected URL — redirect to /password page"),
      @ApiResponse(responseCode = "302", description = "URL not found — redirect to /not-found-url"),
      @ApiResponse(responseCode = "302", description = "URL expired — redirect to /expired"),
      @ApiResponse(responseCode = "204", description = "Prefetch request ignored")
  })
  @GetMapping(path = ApiPath.REDIRECT)
  public ResponseEntity<Void> redirect(
      @Parameter(description = "Short URL code", example = "abc123") @PathVariable String shortUrl,
      HttpServletRequest request) {

    if (isPrefetchRequest(request)) {
      return ResponseEntity.noContent().build();
    }

    long start = System.currentTimeMillis();
    RedirectResponse redirectResponse = urlService.resolveDestination(shortUrl);

    if (Objects.nonNull(redirectResponse.getUrlId())) {
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
    }

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

  @Operation(summary = "Not found handler", description = "Target for internal redirects when a short URL does not exist.")
  @ApiResponse(responseCode = "404", description = "Short URL not found")
  @GetMapping(path = ApiPath.NOT_FOUND)
  public ResponseEntity<Void> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @Operation(summary = "Expired URL handler", description = "Target for internal redirects when a short URL has passed its expiry date.")
  @ApiResponse(responseCode = "410", description = "URL has expired")
  @GetMapping(path = ApiPath.EXPIRED)
  public ResponseEntity<Void> expired() {
    return ResponseEntity.status(HttpStatus.GONE).build();
  }

  @Operation(summary = "Verify password for a protected URL", description = "Submits a password for a password-protected short URL. On success, returns the destination URL as JSON so the frontend can navigate. On failure, returns 401.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Password correct — destination URL returned in response body"),
      @ApiResponse(responseCode = "401", description = "Incorrect password")
  })
  @PostMapping(
      path = ApiPath.VERIFY_PASSWORD,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public RestSingleResponse<VerifyPasswordResponse> verifyPassword(
      @Parameter(description = "Short URL code", example = "abc123") @PathVariable String shortUrl,
      @RequestBody VerifyPasswordRequest request) {
    String destinationUrl = urlService.verifyPassword(shortUrl, request.getPassword());
    return this.toResponseSingleResponse(VerifyPasswordResponse.builder()
            .destinationUrl(destinationUrl)
        .build());
  }

  @Operation(summary = "List user URLs", description = "Returns a paginated list of the authenticated user's shortened URLs. Supports filtering by active/expired status and protection. Excludes soft-deleted URLs.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Paginated list of URLs"),
      @ApiResponse(responseCode = "401", description = "Unauthenticated")
  })
  @GetMapping(path = ApiPath.LIST_USER_URLS, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestListContentResponse<UrlResponse> getUserUrls(
      @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "Sort field: createdDate, expiryDate, shortUrl", example = "createdDate") @RequestParam(defaultValue = "createdDate") String sortBy,
      @Parameter(description = "Sort direction: asc or desc", example = "desc") @RequestParam(defaultValue = "desc") String sortDir,
      @Parameter(description = "Filter by active (true) or expired (false). Omit for all.") @RequestParam(required = false) Boolean active,
      @Parameter(description = "Filter by password-protected (true) or not (false). Omit for all.") @RequestParam(required = false) Boolean isProtected) {
    List<UrlResponse> content = urlService.getUserUrls(page, size, sortBy, sortDir, active, isProtected);
    long total = urlService.countUserUrls(active, isProtected);
    return this.toResponseListContentResponse(content, page, size, total);
  }

  @Operation(summary = "Create a short URL", description = "Creates a shortened URL. Optionally supports custom short codes, password protection, and expiry. Requires Clerk JWT authentication.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Short URL created successfully"),
      @ApiResponse(responseCode = "400", description = "Validation error — blank destination URL, custom short URL too long, or expiry date in the past"),
      @ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @ApiResponse(responseCode = "409", description = "Custom short URL already taken")
  })
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
