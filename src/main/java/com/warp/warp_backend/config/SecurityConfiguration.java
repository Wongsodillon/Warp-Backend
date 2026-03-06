package com.warp.warp_backend.config;

import com.warp.warp_backend.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Autowired
  private ApplicationProperties applicationProperties;

  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withJwkSetUri(applicationProperties.getJwksUri())
        .build();

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator()));

    return decoder;
  }

  @Bean
  @Order(1)
  public SecurityFilterChain redirectFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/{shortUrl}")
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain apiFilterChain(HttpSecurity http,
      ClerkJwtAuthenticationConverter converter) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));

    return http.build();
  }
}
