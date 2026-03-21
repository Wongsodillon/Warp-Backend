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
    private Long urlId;
    private String shortUrl;
    private UrlStatus status;
    private String destinationUrl;
    private Long expiryDate;
    private boolean isProtected;
}
