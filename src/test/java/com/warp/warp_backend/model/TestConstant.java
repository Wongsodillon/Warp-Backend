package com.warp.warp_backend.model;

public class TestConstant {

  public static final String TEST_TOKEN = "TEST-TOKEN";

  public static final String TEST_CLERK_USER_ID = "test_clerk_user_id";

  public static final String DESTINATION_URL = "https://example.com";

  public static final String SHORT_URL = "WARP-TEST-SHORT-URL";

  public static final String PROTECTED_SHORT_URL = "WARP-TEST-PROTECTED-URL";
  public static final String TEST_PASSWORD = "test-password";

  public static final String LOCALHOST_URL = "http://localhost/path";
  public static final String PRIVATE_IP_URL = "http://192.168.1.1/path";
  public static final String RAW_IP_URL = "http://8.8.8.8/path";
  public static final String MULTICAST_IP_URL = "http://224.0.0.1/path";
  public static final String FTP_URL = "ftp://example.com/file";
  public static final String MALFORMED_URL = "not-a-url";

  public static final String CUSTOM_SHORT_URL_VALUE = "my-custom-link";
  public static final String DUPLICATE_CUSTOM_SHORT_URL = "conflict-url";
  public static final String TOO_LONG_CUSTOM_SHORT_URL = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 31 chars
  public static final String INVALID_FORMAT_CUSTOM_SHORT_URL = "bad url!";
}
