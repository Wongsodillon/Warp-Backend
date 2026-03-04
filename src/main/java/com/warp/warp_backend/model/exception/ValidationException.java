package com.warp.warp_backend.model.exception;

import com.warp.warp_backend.model.common.ErrorCode;

public class ValidationException extends BaseException {

  public ValidationException(ErrorCode errorCode) {
    super(errorCode);
  }

  public ValidationException(ErrorCode errorCode, String errorMessage) {
    super(errorCode, errorMessage);
  }
}
