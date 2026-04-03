package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.response.BreakdownResponse;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.model.response.TimeSeriesResponse;
import com.warp.warp_backend.model.response.TopUrlsTimeSeriesResponse;
import com.warp.warp_backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "Click analytics endpoints")
@RestController
public class AnalyticsController extends BaseController {

  @Autowired
  private AnalyticsService analyticsService;

  @Operation(summary = "Get click timeseries",
      description = "Returns time-bucketed click counts for a URL or all user URLs")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(path = ApiPath.ANALYTICS_CLICKS_TIMESERIES, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestSingleResponse<TimeSeriesResponse> getClicksTimeSeries(
      @Parameter(description = "Short URL code (omit for all user URLs)")
      @RequestParam(required = false) String shortUrl,
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    TimeSeriesResponse response = analyticsService.getClicksTimeSeries(shortUrl, period);
    return toResponseSingleResponse(response);
  }

  @Operation(summary = "Get top URLs click timeseries",
      description = "Returns top URLs by total clicks with individual zero-filled time-series for the current user")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(path = ApiPath.ANALYTICS_CLICKS_TOP_URLS_TIMESERIES, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestSingleResponse<TopUrlsTimeSeriesResponse> getTopUrlsTimeSeries(
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    TopUrlsTimeSeriesResponse response = analyticsService.getTopUrlsTimeSeries(period);
    return toResponseSingleResponse(response);
  }

  @Operation(summary = "Get clicks by device type",
      description = "Returns click breakdown by device for a URL or all user URLs")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(path = ApiPath.ANALYTICS_CLICKS_DEVICES, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestSingleResponse<BreakdownResponse> getDeviceBreakdown(
      @Parameter(description = "Short URL code (omit for all user URLs)")
      @RequestParam(required = false) String shortUrl,
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    BreakdownResponse response = analyticsService.getDeviceBreakdown(shortUrl, period);
    return toResponseSingleResponse(response);
  }

  @Operation(summary = "Get clicks by country",
      description = "Returns click breakdown by country for a URL or all user URLs")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(path = ApiPath.ANALYTICS_CLICKS_COUNTRIES, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestSingleResponse<BreakdownResponse> getCountryBreakdown(
      @Parameter(description = "Short URL code (omit for all user URLs)")
      @RequestParam(required = false) String shortUrl,
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    BreakdownResponse response = analyticsService.getCountryBreakdown(shortUrl, period);
    return toResponseSingleResponse(response);
  }

  @Operation(summary = "Get clicks by browser",
      description = "Returns click breakdown by browser for a URL or all user URLs")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(path = ApiPath.ANALYTICS_CLICKS_BROWSERS, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestSingleResponse<BreakdownResponse> getBrowserBreakdown(
      @Parameter(description = "Short URL code (omit for all user URLs)")
      @RequestParam(required = false) String shortUrl,
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    BreakdownResponse response = analyticsService.getBrowserBreakdown(shortUrl, period);
    return toResponseSingleResponse(response);
  }

  @Operation(summary = "Get clicks by referrer source",
      description = "Returns click breakdown by referrer for a URL or all user URLs")
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(path = ApiPath.ANALYTICS_CLICKS_SOURCES, produces = MediaType.APPLICATION_JSON_VALUE)
  public RestSingleResponse<BreakdownResponse> getSourceBreakdown(
      @Parameter(description = "Short URL code (omit for all user URLs)")
      @RequestParam(required = false) String shortUrl,
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    BreakdownResponse response = analyticsService.getSourceBreakdown(shortUrl, period);
    return toResponseSingleResponse(response);
  }
}
