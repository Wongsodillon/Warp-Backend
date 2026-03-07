package com.warp.warp_backend.util;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;

@Component
public class UrlValidationUtil {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  public void validateDestinationUrl(String url) {
    if (!isValid(url)) {
      throw new ValidationException(ErrorCode.DESTINATION_URL_IS_INVALID);
    }
  }

  private boolean isValid(String url) {
    URL parsed;
    try {
      parsed = new URI(url).toURL();
    } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
      return false;
    }
    return isAllowedScheme(parsed.getProtocol()) && isValidHost(parsed.getHost());
  }

  private boolean isAllowedScheme(String scheme) {
    return ALLOWED_SCHEMES.contains(scheme.toLowerCase());
  }

  private boolean isValidHost(String host) {
    if (host == null || host.isBlank()) {
      return false;
    }
    InetAddress address;
    try {
      address = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      return false;
    }
    return !isPrivateAddress(address) && !isRawIpAddress(address, host);
  }

  private boolean isPrivateAddress(InetAddress address) {
    return address.isLoopbackAddress()
        || address.isSiteLocalAddress()
        || address.isLinkLocalAddress()
        || address.isAnyLocalAddress();
  }

  private boolean isRawIpAddress(InetAddress address, String host) {
    return address.getHostAddress().equalsIgnoreCase(host);
  }
}
