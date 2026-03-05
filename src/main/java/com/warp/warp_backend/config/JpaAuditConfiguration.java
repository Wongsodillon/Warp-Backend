package com.warp.warp_backend.config;

import com.warp.warp_backend.model.constant.ConstantValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfiguration {
  @Bean
  public AuditorAware<String> jpaAuditorAware() {
    return () -> {
      Authentication authentication =
          SecurityContextHolder.getContext().getAuthentication();
      if (Objects.isNull(authentication)) {
        return Optional.of(ConstantValue.SYSTEM);
      }
      return Optional.ofNullable(authentication.getName())
          .or(() -> Optional.of(ConstantValue.SYSTEM));
    };
  }
}
