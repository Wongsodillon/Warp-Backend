package com.warp.warp_backend.model.general;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAgentInfo {
    private String browser;
    private String deviceType;
}
