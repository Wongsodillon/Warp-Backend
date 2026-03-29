package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.model.response.TimeSeriesResponse;
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
      @Parameter(description = "URL ID (omit for all user URLs)")
      @RequestParam(required = false) Long urlId,
      @Parameter(description = "Time period: 1h, 6h, 1d, 7d, 30d")
      @RequestParam String period) {
    TimeSeriesResponse response = analyticsService.getClicksTimeSeries(urlId, period);
    return toResponseSingleResponse(response);
  }
}
