package com.warp.warp_backend.model.exception;

import com.warp.warp_backend.model.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {

  private static final long serialVersionUID = -4260915871045723774L;

  private int httpStatus;

  private String errorCode;
  private String errorMessage;
  private String errorValue;
  private String errorValueSystem;
  private String correctValue;

  public BaseException(ErrorCode errorCode, Throwable e) {
    super(e);
    this.errorCode = errorCode.getCode();
    this.errorMessage = errorCode.getDescription();
    this.httpStatus = errorCode.getHttpStatus();
  }

  public BaseException(ErrorCode errorCode) {
    super(errorCode.getDescription());
    this.errorCode = errorCode.getCode();
    this.errorMessage = errorCode.getDescription();
    this.httpStatus = errorCode.getHttpStatus();
  }

  public BaseException(ErrorCode errorCode, String errorMessage) {
    super(errorMessage);
    this.errorCode = errorCode.getCode();
    this.errorMessage = errorMessage;
    this.httpStatus = errorCode.getHttpStatus();
  }
}
