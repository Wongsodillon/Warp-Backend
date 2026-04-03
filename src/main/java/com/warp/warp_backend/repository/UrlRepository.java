package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long>, UrlCustomRepository {

  Optional<Url> findByShortUrl(String shortUrl);

  Optional<Url> findByShortUrlAndDeletedDateIsNull(String shortUrl);

  @Query(value = "SELECT nextval('urls_id_seq')", nativeQuery = true)
  Long getNextId();

  @Query("SELECT u.id FROM Url u WHERE u.userId = :userId AND u.deletedDate IS NULL")
  List<Long> findAllIdsByUserId(@Param("userId") Long userId);

  void deleteByShortUrl(String shortUrl);
}
