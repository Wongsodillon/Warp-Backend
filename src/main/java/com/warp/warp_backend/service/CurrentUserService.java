package com.warp.warp_backend.service;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.entity.User;
import com.warp.warp_backend.model.exception.BaseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CurrentUserService {

  public User getCurrentUser() {

    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

    if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
      throw new BaseException(ErrorCode.UNAUTHENTICATED);
    }

    Object principal = authentication.getPrincipal();

    if (!(principal instanceof User user)) {
      throw new BaseException(ErrorCode.UNAUTHENTICATED);
    }

    return user;
  }

  public Long getCurrentUserId() {
    return getCurrentUser().getId();
  }
}
