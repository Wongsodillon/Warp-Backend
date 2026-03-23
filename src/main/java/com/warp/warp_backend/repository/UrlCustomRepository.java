package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.entity.Url;

import java.util.List;

public interface UrlCustomRepository {

  List<Url> findUserUrls(Long userId, Boolean active, Boolean isProtected,
      int page, int size, String sortBy, String sortDir);

  long countUserUrls(Long userId, Boolean active, Boolean isProtected);
}
