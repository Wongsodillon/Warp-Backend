package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long>, UrlCustomRepository {

  Optional<Url> findByShortUrl(String shortUrl);

  @Query(value = "SELECT nextval('urls_id_seq')", nativeQuery = true)
  Long getNextId();

  void deleteByShortUrl(String shortUrl);
}
