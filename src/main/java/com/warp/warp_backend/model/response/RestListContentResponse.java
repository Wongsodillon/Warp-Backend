package com.warp.warp_backend.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Schema(description = "Response envelope wrapping a paginated list of items")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RestListContentResponse<T> extends RestBaseResponse {

  @Schema(description = "List of items in the current page")
  private List<T> content;

  @Schema(description = "Current page number (0-based)", example = "0")
  private int page;

  @Schema(description = "Number of items per page", example = "10")
  private int size;

  @Schema(description = "Total number of items across all pages", example = "42")
  private long totalElements;

  @Schema(description = "Total number of pages", example = "5")
  private int totalPages;
}
