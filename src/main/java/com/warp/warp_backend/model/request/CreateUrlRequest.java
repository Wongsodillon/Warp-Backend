package com.warp.warp_backend.model.request;

import com.warp.warp_backend.model.annotation.constraint.NotBlank;
import com.warp.warp_backend.model.common.ErrorCode;
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

  @NotBlank(errorCode = ErrorCode.DESTINATION_URL_IS_BLANK)
  private String destinationUrl;

  private OffsetDateTime expiresAt;
}
