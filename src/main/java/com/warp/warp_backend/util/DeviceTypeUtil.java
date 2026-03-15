package com.warp.warp_backend.util;

import java.util.Objects;

public class DeviceTypeUtil {

  public static String parse(String userAgent) {
    if (Objects.isNull(userAgent)) {
      return "desktop";
    }
    String ua = userAgent.toLowerCase();
    if (ua.contains("tablet") || ua.contains("ipad")) {
      return "tablet";
    }
    if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipod")) {
      return "mobile";
    }
    return "desktop";
  }
}
