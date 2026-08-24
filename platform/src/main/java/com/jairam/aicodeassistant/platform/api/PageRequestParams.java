package com.jairam.aicodeassistant.platform.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Bindable, validated pagination request parameters for list endpoints.
 *
 * <p>Bounds are enforced here (page &ge; 0, size in [1, 100]) so no endpoint can be used to request
 * an unbounded page — a common resource-exhaustion vector. Controllers translate this into the
 * persistence layer's own paging type.
 */
public record PageRequestParams(
    @Min(0) int page, @Min(1) @Max(100) int size, String sort, SortDirection direction) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public PageRequestParams {
    if (size == 0) {
      size = DEFAULT_SIZE;
    }
    if (direction == null) {
      direction = SortDirection.ASC;
    }
  }

  public static PageRequestParams firstPage() {
    return new PageRequestParams(0, DEFAULT_SIZE, null, SortDirection.ASC);
  }

  public enum SortDirection {
    ASC,
    DESC
  }
}
