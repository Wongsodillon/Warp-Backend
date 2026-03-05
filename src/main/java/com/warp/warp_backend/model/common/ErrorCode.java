package com.warp.warp_backend.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {


  REQUEST_ID_IS_BLANK(400, "ERR-400001", "requestId parameter can't be null"),
  DESTINATION_URL_IS_BLANK(400, "ERR-400002", "Destination URL can't be empty"),

  DESTINATION_URL_NOT_FOUND(404, "ERR-404001", "URL not found"),

  UNSPECIFIED(500, "ERR-500001", "Unspecified error");

  private final int httpStatus;

  private final String code;

  private final String description;
}
