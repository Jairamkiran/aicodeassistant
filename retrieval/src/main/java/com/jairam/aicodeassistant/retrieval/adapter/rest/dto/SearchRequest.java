package com.jairam.aicodeassistant.retrieval.adapter.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Code-search request. Scoped to an organization; optionally narrowed to one repository and/or one
 * programming language.
 *
 * @param organizationId tenant scope (required)
 * @param repositoryId optional single-repo filter
 * @param language optional language filter (e.g. {@code "java"}), matched case-insensitively
 * @param query the search text
 * @param limit max results (1–50; null applies the server default)
 */
public record SearchRequest(
    @NotNull UUID organizationId,
    UUID repositoryId,
    String language,
    @NotBlank String query,
    @Min(1) @Max(50) Integer limit) {

  /** Effective limit: the requested value, or the query default when unspecified. */
  public int effectiveLimit() {
    return limit == null
        ? com.jairam.aicodeassistant.retrieval.search.SearchQuery.DEFAULT_LIMIT
        : limit;
  }
}
