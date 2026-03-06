package com.warp.warp_backend.util;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.User;
import com.warp.warp_backend.model.exception.BaseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class AuthHelper {

  public static User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (Objects.isNull(authentication) || !(authentication.getPrincipal() instanceof User user)) {
      throw new BaseException(ErrorCode.UNAUTHENTICATED);
    }
    return user;
  }

  public static Long getCurrentUserId() {
    return getCurrentUser().getId();
  }
}
