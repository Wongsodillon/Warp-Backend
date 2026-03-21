package com.warp.warp_backend.util;

import com.warp.warp_backend.model.constant.DeviceType;
import com.warp.warp_backend.model.constant.HttpHeader;
import com.warp.warp_backend.model.general.UserAgentInfo;
import org.springframework.stereotype.Component;
import ua_parser.Client;
import ua_parser.Parser;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Component
public class UserAgentUtil {

  private static final String WWW_PREFIX    = "www.";

  private final Parser parser;

  public UserAgentUtil() throws IOException {
    this.parser = new Parser();
  }

  public UserAgentInfo parseUserAgent(String userAgent) {
    if (Objects.isNull(userAgent)) {
      return UserAgentInfo.builder().browser(DeviceType.UNKNOWN).deviceType(DeviceType.UNKNOWN).build();
    }
    Client client = parser.parse(userAgent);

    String browser = client.userAgent.family;

    String deviceFamily = client.device.family.toLowerCase();
    String osFamily = client.os.family.toLowerCase();
    String deviceType;
    if (deviceFamily.contains("tablet") || deviceFamily.equals("ipad")) {
      deviceType = DeviceType.TABLET;
    } else if (osFamily.equals("ios") || osFamily.equals("android")
        || deviceFamily.contains("phone") || deviceFamily.contains("mobile")
        || deviceFamily.equals("iphone")) {
      deviceType = DeviceType.MOBILE;
    } else {
      deviceType = DeviceType.DESKTOP;
    }

    return UserAgentInfo.builder().browser(browser).deviceType(deviceType).build();
  }

  public String parseReferrer(String referer) {
    if (referer == null || referer.isBlank()) return null;
    try {
      String host = extractHost(referer);
      if (host == null) return null;
      host = host.toLowerCase();
      return host.startsWith(WWW_PREFIX) ? host.substring(WWW_PREFIX.length()) : host;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  public String extractClientIp(HttpServletRequest request) {
    String xff = request.getHeader(HttpHeader.X_FORWARDED_FOR);
    if (Objects.nonNull(xff) && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    String xRealIp = request.getHeader(HttpHeader.X_REAL_IP);
    if (Objects.nonNull(xRealIp) && !xRealIp.isBlank()) {
      return xRealIp.trim();
    }
    return request.getRemoteAddr();
  }

  private String extractHost(String referer) throws URISyntaxException {
    String host = new URI(referer).getHost();
    if (host == null) {
      host = new URI("http://" + referer).getHost();
    }
    return host;
  }

}
