package com.warp.warp_backend.model.general;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CachedUrl {

  private String destinationUrl;
  private Long expiryDate;
  private boolean isProtected;
  private boolean disabled;
  private boolean deleted;
}
