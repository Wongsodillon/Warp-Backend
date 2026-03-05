package com.warp.warp_backend.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUrlRequest {

  @NotBlank
  private String destinationUrl;

  private OffsetDateTime expiresAt;
}
