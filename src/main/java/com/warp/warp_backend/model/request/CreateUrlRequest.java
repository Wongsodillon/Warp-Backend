package com.warp.warp_backend.model.request;

import com.warp.warp_backend.model.annotation.constraint.MaxLength;
import com.warp.warp_backend.model.annotation.constraint.NotBlank;
import com.warp.warp_backend.model.common.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Schema(description = "Request body for creating a short URL")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUrlRequest {

  @Schema(description = "The full destination URL to shorten", example = "https://example.com/some/long/path", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(errorCode = ErrorCode.DESTINATION_URL_IS_BLANK)
  private String destinationUrl;

  @Schema(description = "Optional expiry date-time for the short URL (must be in the future)", example = "2026-12-31T23:59:59Z")
  private OffsetDateTime expiresAt;

  @Schema(description = "Optional password to protect the short URL", example = "s3cr3t")
  private String password;

  @Schema(description = "Optional custom short code (max 30 characters). If omitted, one is auto-generated.", example = "my-link", maxLength = 30)
  @MaxLength(value = 30, errorCode = ErrorCode.CUSTOM_SHORT_URL_TOO_LONG)
  private String customShortUrl;
}
