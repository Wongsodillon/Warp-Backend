package com.warp.warp_backend.util;

import org.springframework.stereotype.Component;

@Component
public class Base62 {

  private static final String ALPHABET =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public static String encode(long value) {

    StringBuilder sb = new StringBuilder();

    while (value > 0) {
      int remainder = (int) (value % 62);
      sb.append(ALPHABET.charAt(remainder));
      value /= 62;
    }

    return sb.reverse().toString();
  }
}
