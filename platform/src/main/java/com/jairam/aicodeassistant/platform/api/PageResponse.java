package com.jairam.aicodeassistant.platform.api;

import java.util.List;
import java.util.function.Function;

/**
 * Framework-neutral pagination envelope returned by list endpoints.
 *
 * <p>We deliberately do NOT return Spring Data's {@code Page} across the API or across
 * bounded-context boundaries: it couples clients to Spring internals and serialises unstable
 * fields. This record is the stable public contract.
 *
 * @param content the items on this page
 * @param page zero-based page index
 * @param size requested page size
 * @param totalElements total number of matching elements across all pages
 * @param totalPages total number of pages given {@code size}
 * @param <T> item type
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public PageResponse {
    content = content == null ? List.of() : List.copyOf(content);
  }

  /** Maps the page content to another type while preserving pagination metadata. */
  public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
    List<R> mapped = content.stream().<R>map(mapper).toList();
    return new PageResponse<R>(mapped, page, size, totalElements, totalPages);
  }

  /** Convenience factory computing {@code totalPages} from size + total. */
  public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(content, page, size, totalElements, totalPages);
  }
}
