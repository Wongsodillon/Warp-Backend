package com.warp.warp_backend.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Response after successful password verification")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPasswordResponse {

  @Schema(description = "The destination URL the client should navigate to", example = "https://example.com")
  private String destinationUrl;
}
