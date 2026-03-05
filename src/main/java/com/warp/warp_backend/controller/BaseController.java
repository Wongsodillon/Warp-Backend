package com.warp.warp_backend.controller;

import com.warp.warp_backend.model.response.RestSingleResponse;
import com.warp.warp_backend.util.RequestContextHelper;

public class BaseController {

  public <T> RestSingleResponse<T> toResponseSingleResponse(T data) {
    return RestSingleResponse.<T>builder()
        .requestId(RequestContextHelper.getRequestId())
        .success(true)
        .value(data)
        .build();
  }
}
