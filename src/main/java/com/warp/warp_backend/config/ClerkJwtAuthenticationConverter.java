package com.warp.warp_backend.config;

import com.warp.warp_backend.model.entity.User;
import com.warp.warp_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClerkJwtAuthenticationConverter implements
    Converter<Jwt, AbstractAuthenticationToken> {

  @Autowired
  private UserRepository userRepository;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String clerkUserId = jwt.getSubject();

    User user = userRepository.findByClerkUserId(clerkUserId)
        .orElseGet(() -> {
          User newUser = new User();
          newUser.setClerkUserId(clerkUserId);
          return userRepository.save(newUser);
        });

    return new UsernamePasswordAuthenticationToken(user, null, List.of());
  }
}
