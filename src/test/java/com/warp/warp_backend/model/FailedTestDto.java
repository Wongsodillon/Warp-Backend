package com.warp.warp_backend.model;

import com.warp.warp_backend.model.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedTestDto {

  private String path;
  private String customErrorCode;
  private String customErrorMessage;

  @Builder.Default
  private Object[] pathVariables = {};

  @Builder.Default
  private boolean useAuth = true;

  private MockMvc mockMvc;

  private HttpMethod httpMethod;

  private Object body;

  private ErrorCode errorCode;

  private HttpStatus httpStatus;

  private MultiValueMap<String, String> additionalParams;
}
