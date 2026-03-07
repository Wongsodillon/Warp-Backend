package com.warp.warp_backend.service;

import com.warp.warp_backend.model.constant.ConstantValue;
import com.warp.warp_backend.model.entity.User;
import com.warp.warp_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public User resolveOrCreateUser(String clerkUserId) {
    return userRepository.findByClerkUserId(clerkUserId)
        .orElseGet(() -> userRepository.save(User.builder()
            .clerkUserId(clerkUserId)
            .role(ConstantValue.USER)
            .build()));
  }
}
