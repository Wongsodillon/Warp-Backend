package com.warp.warp_backend.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.warp.warp_backend.model.constant.GeoConstant;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;

@Service
public class GeoLocationService {

  private static final Logger log = LoggerFactory.getLogger(GeoLocationService.class);

  private DatabaseReader reader;

  @PostConstruct
  public void init() {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(GeoConstant.MMDB_FILE)) {
      if (Objects.isNull(stream)) {
        log.warn("{} not found on classpath — geo-resolution disabled", GeoConstant.MMDB_FILE);
        return;
      }
      byte[] bytes = stream.readAllBytes();
      this.reader = new DatabaseReader.Builder(new ByteArrayInputStream(bytes)).build();
      log.info("{} loaded successfully", GeoConstant.MMDB_FILE);
    } catch (IOException e) {
      log.warn("Failed to load {} — geo-resolution disabled", GeoConstant.MMDB_FILE, e);
    }
  }

  @PreDestroy
  public void destroy() {
    if (Objects.nonNull(reader)) {
      try {
        reader.close();
      } catch (IOException ignored) {
      }
    }
  }

  public boolean isAvailable() {
    return Objects.nonNull(reader);
  }

  public Optional<String> resolveCountryCode(String ip) {
    if (Objects.isNull(reader) || Objects.isNull(ip) || ip.isBlank()) {
      return Optional.empty();
    }
    try {
      InetAddress addr = InetAddress.getByName(ip.trim());
      if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()) {
        return Optional.empty();
      }
      return Optional.ofNullable(reader.country(addr).getCountry().getIsoCode());
    } catch (AddressNotFoundException e) {
      return Optional.empty();
    } catch (IOException | GeoIp2Exception e) {
      log.debug("Geo-resolution failed for IP {}: {}", ip, e.getMessage());
      return Optional.empty();
    }
  }
}
