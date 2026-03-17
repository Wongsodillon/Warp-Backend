package com.warp.warp_backend.util;

import com.warp.warp_backend.model.general.UserAgentInfo;
import org.springframework.stereotype.Component;
import ua_parser.Client;
import ua_parser.Parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Component
public class UserAgentUtil {

  private final Parser parser;

  public UserAgentUtil() throws IOException {
    this.parser = new Parser();
  }

  public UserAgentInfo parseUserAgent(String userAgent) {
    if (Objects.isNull(userAgent)) {
      return UserAgentInfo.builder().browser("Unknown").deviceType("Unknown").build();
    }
    Client client = parser.parse(userAgent);

    String browser = client.userAgent.family;

    String deviceFamily = client.device.family.toLowerCase();
    String osFamily = client.os.family.toLowerCase();
    String deviceType;
    if (deviceFamily.contains("tablet") || deviceFamily.equals("ipad")) {
      deviceType = "Tablet";
    } else if (osFamily.equals("ios") || osFamily.equals("android")
        || deviceFamily.contains("phone") || deviceFamily.contains("mobile")
        || deviceFamily.equals("iphone")) {
      deviceType = "Mobile";
    } else {
      deviceType = "Desktop";
    }

    return UserAgentInfo.builder().browser(browser).deviceType(deviceType).build();
  }

  public String parseReferrer(String referer) {
    if (referer == null || referer.isBlank()) return null;
    try {
      String host = extractHost(referer);
      if (host == null) return null;
      host = host.toLowerCase();
      return host.startsWith("www.") ? host.substring(4) : host;
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private String extractHost(String referer) throws URISyntaxException {
    String host = new URI(referer).getHost();
    if (host == null) {
      host = new URI("http://" + referer).getHost();
    }
    return host;
  }

}
