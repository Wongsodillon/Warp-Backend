package com.warp.warp_backend.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(description = "Base response envelope for all API responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(builderMethodName = "parentBuilder")
public class RestBaseResponse {

  @Schema(description = "Unique identifier for this request, useful for tracing", example = "550e8400-e29b-41d4-a716-446655440000")
  private String requestId;

  @Schema(description = "Human-readable error message (null on success)")
  private String errorMessage;

  @Schema(description = "Machine-readable error code (null on success)", example = "DESTINATION_URL_IS_BLANK")
  private String errorCode;

  @Schema(description = "Whether the request succeeded", example = "true")
  @Builder.Default
  private boolean success = true;
}
