package com.warp.warp_backend.model.constant;

public final class ApiPath {
  public static final String REDIRECT = "/{shortUrl}";

  public static final String SHORTEN_URL = "/api/shorten";

  public static final String VERIFY_PASSWORD = "/{shortUrl}/verify";

  public static final String ACTUATOR_HEALTH = "/actuator/health";
}
