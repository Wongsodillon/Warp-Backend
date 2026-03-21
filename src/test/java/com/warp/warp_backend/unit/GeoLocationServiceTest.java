package com.warp.warp_backend.unit;

import com.warp.warp_backend.service.GeoLocationService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoLocationServiceTest {

  private GeoLocationService geoLocationService;

  @BeforeEach
  void setUp() {
    geoLocationService = new GeoLocationService();
    geoLocationService.init();
  }

  @Test
  void resolveCountryCode_nullIp_returnsEmpty() {
    assertThat(geoLocationService.resolveCountryCode(null)).isEmpty();
  }

  @Test
  void resolveCountryCode_blankIp_returnsEmpty() {
    assertThat(geoLocationService.resolveCountryCode("")).isEmpty();
  }

  @Test
  void resolveCountryCode_loopbackIp_returnsEmpty() {
    assertThat(geoLocationService.resolveCountryCode("127.0.0.1")).isEmpty();
  }

  @Test
  void resolveCountryCode_privateIp_returnsEmpty() {
    assertThat(geoLocationService.resolveCountryCode("192.168.1.1")).isEmpty();
  }

  @Test
  void resolveCountryCode_readerAbsent_returnsEmpty() {
    GeoLocationService serviceWithoutMmdb = new GeoLocationService();
    // do NOT call init() — reader stays null
    assertThat(serviceWithoutMmdb.resolveCountryCode("8.8.8.8")).isEmpty();
  }

  @Test
  void resolveCountryCode_googleDns_returnsUs() {
    Assumptions.assumeTrue(geoLocationService.isAvailable(),
        "GeoLite2-Country.mmdb not present — skipping live lookup test");
    Optional<String> result = geoLocationService.resolveCountryCode("8.8.8.8");
    assertThat(result).contains("US");
  }
}
