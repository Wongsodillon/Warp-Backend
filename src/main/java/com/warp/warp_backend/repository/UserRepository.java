package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByClerkUserId(String clerkUserId);

}
