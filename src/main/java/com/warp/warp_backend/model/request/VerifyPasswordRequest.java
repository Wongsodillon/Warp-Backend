package com.warp.warp_backend.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for verifying a password-protected short URL")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPasswordRequest {

  @Schema(description = "The password for the protected short URL", example = "s3cr3t", requiredMode = Schema.RequiredMode.REQUIRED)
  private String password;
}
