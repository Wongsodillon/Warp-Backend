package com.warp.warp_backend.service;

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
    return userRepository.findByClerkUserId("dev-user-1")
        .orElseGet(() -> {
          User user = new User();
          user.setClerkUserId(clerkUserId);
          user.setRole("USER");
          return userRepository.save(user);
        });
  }
}
