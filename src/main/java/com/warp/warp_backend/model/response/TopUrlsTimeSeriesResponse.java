package com.warp.warp_backend.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopUrlsTimeSeriesResponse {
  private String period;
  private String bucket;
  private List<TopUrlTimeSeriesEntry> urls;
}
