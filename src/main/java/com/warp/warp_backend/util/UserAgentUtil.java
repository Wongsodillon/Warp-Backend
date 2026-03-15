package com.warp.warp_backend.util;

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

  public String parseBrowser(String userAgent) {
    if (Objects.isNull(userAgent)) return "Unknown";
    return parser.parse(userAgent).userAgent.family;
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

  public String parseDeviceType(String userAgent) {
    if (Objects.isNull(userAgent)) return "Unknown";

    Client client = parser.parse(userAgent);
    String deviceFamily = client.device.family.toLowerCase();
    if (deviceFamily.contains("tablet") || deviceFamily.equals("ipad")) return "Tablet";

    String osFamily = client.os.family.toLowerCase();
    if (osFamily.equals("ios") || osFamily.equals("android") || deviceFamily.contains("phone")
        || deviceFamily.contains("mobile") || deviceFamily.equals("iphone")) return "Mobile";
    return "Desktop";
  }
}
