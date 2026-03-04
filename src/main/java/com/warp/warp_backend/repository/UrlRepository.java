package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

  Optional<Url> findByShortUrl(String shortUrl);
}
