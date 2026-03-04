package com.warp.warp_backend.controller.advice;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.common.RestBaseResponse;
import com.warp.warp_backend.model.exception.BaseException;
import com.warp.warp_backend.util.RequestContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {

  private static final Logger LOGGER = LoggerFactory.getLogger(ControllerAdvice.class);

  @Autowired
  private RequestContextHelper requestContextHelper;

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<RestBaseResponse> baseExceptionHandler(
      HttpServletRequest httpServletRequest, BaseException e) {
    LOGGER.error("codeName={} detail ={}", e.getErrorCode(), httpServletRequest.getContextPath(), e);
    return new ResponseEntity<>(createErrorResponse(e.getErrorCode(), e.getErrorMessage()),
        HttpStatus.valueOf(e.getHttpStatus()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<RestBaseResponse> exceptionHandler(
      HttpServletRequest httpServletRequest, Exception e) {
    LOGGER.error("codeName=unspecified-api-response detail ={}",
        httpServletRequest.getContextPath(), e);
    return new ResponseEntity<>(createErrorResponse(ErrorCode.UNSPECIFIED.name(), e.getMessage()),
        HttpStatus.valueOf(ErrorCode.UNSPECIFIED.getHttpStatus()));
  }

  private RestBaseResponse createErrorResponse(String code, String message) {
    return RestBaseResponse.parentBuilder()
        .requestId(requestContextHelper.getRequestId())
        .success(false)
        .errorCode(code)
        .errorMessage(message)
        .build();
  }
}
