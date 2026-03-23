package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.response.RestBaseResponse;
import com.warp.warp_backend.model.response.RestListContentResponse;
import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.util.RequestContextHelper;

import java.util.List;

public class BaseController {

  public RestBaseResponse toBaseResponse() {
    return RestBaseResponse.parentBuilder()
        .requestId(RequestContextHelper.getRequestId())
        .success(true)
        .build();
  }

  public <T> RestSingleResponse<T> toResponseSingleResponse(T data) {
    return RestSingleResponse.<T>builder()
        .requestId(RequestContextHelper.getRequestId())
        .success(true)
        .value(data)
        .build();
  }

  public <T> RestListContentResponse<T> toResponseListContentResponse(
      List<T> content, int page, int size, long totalElements) {
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    return RestListContentResponse.<T>builder()
        .requestId(RequestContextHelper.getRequestId())
        .success(true)
        .content(content)
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .build();
  }
}
