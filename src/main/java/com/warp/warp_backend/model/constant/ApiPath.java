package com.warp.warp_backend.model.constant;

public final class ApiPath {
  public static final String REDIRECT = "/{shortUrl}";

  public static final String SHORTEN_URL = "/api/shorten";

  public static final String LIST_USER_URLS = "/api/urls";

  public static final String DELETE_URL = "/api/urls/{shortUrl}";

  public static final String VERIFY_PASSWORD = "/{shortUrl}/verify";

  public static final String ACTUATOR_HEALTH = "/actuator/health";
  public static final String ACTUATOR_METRICS = "/actuator/metrics";
  
  public static final String ANALYTICS_CLICKS_TIMESERIES = "/api/v1/analytics/clicks/timeseries";

  public static final String NOT_FOUND = "/not-found-url";
  public static final String EXPIRED = "/expired";
  public static final String PASSWORD = "/password";
}
