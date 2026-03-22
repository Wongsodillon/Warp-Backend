package com.warp.warp_backend.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Created short URL details")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUrlResponse {

  @Schema(description = "The full short URL (includes domain)", example = "https://warp.ly/abc123")
  private String shortUrl;

  @Schema(description = "The original destination URL", example = "https://example.com/some/long/path")
  private String destinationUrl;
}
