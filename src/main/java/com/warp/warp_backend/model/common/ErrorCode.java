package com.warp.warp_backend.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {


  REQUEST_ID_IS_BLANK(400, "ERR-400001", "requestId parameter can't be null"),
  DESTINATION_URL_IS_BLANK(400, "ERR-400002", "Destination URL can't be empty"),
  DESTINATION_URL_IS_INVALID(400, "ERR-400003", "Destination URL is invalid"),
  CUSTOM_SHORT_URL_TOO_LONG(400, "ERR-400004", "Custom short URL must not exceed 30 characters"),
  CUSTOM_SHORT_URL_INVALID_FORMAT(400, "ERR-400005", "Custom short URL may only contain letters, digits, hyphens, and underscores"),

  UNAUTHENTICATED(401,"ERR-401001", "Unauthenticated User"),

  URL_ACCESS_FORBIDDEN(403, "ERR-403001", "You do not have access to this URL"),
  INVALID_PASSWORD(401, "ERR-401002", "Invalid password"),

  EXPIRES_AT_IN_THE_PAST(400, "ERR-400006", "Expiry date must be in the future"),

  CUSTOM_SHORT_URL_ALREADY_EXISTS(409, "ERR-409001", "Custom short URL is already in use"),

  DESTINATION_URL_NOT_FOUND(404, "ERR-404001", "URL not found"),

  URL_EXPIRED(410, "ERR-410001", "URL has expired"),

  INVALID_PERIOD(400, "ERR-400007", "Period must be one of: 1h, 6h, 1d, 7d, 30d"),

  INVALID_LIMIT(400, "ERR-400008", "Limit must be between 1 and the configured maximum"),

  UNSPECIFIED(500, "ERR-500001", "Unspecified error");

  private final int httpStatus;

  private final String code;

  private final String description;
}
