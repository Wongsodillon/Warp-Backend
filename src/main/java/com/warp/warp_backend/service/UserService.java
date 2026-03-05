package com.warp.warp_backend.service;

import com.warp.warp_backend.model.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

  User resolveOrCreateUser(String clerkUserId);
}
