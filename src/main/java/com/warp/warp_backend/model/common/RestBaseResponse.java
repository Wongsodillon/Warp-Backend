package com.warp.warp_backend.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(builderMethodName = "parentBuilder")
public class RestBaseResponse {

  private String requestId;
  private String errorMessage;
  private String errorCode;
  @Builder.Default
  private boolean success = true;
}
