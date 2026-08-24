package com.jairam.aicodeassistant.retrieval.search;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A code-search request, always scoped to an organization (multi-tenant isolation) and optionally
 * narrowed to a single repository and/or a single programming language.
 *
 * @param organizationId tenant scope (required)
 * @param repositoryId optional single-repo filter (null = all repos in the org)
 * @param language optional language filter (null/blank = all languages), matched lower-cased
 * @param text the natural-language / keyword query
 * @param limit max results to return (topK), bounded
 */
public record SearchQuery(
    UUID organizationId, UUID repositoryId, String language, String text, int limit) {

  public static final int DEFAULT_LIMIT = 10;
  public static final int MAX_LIMIT = 50;

  public SearchQuery {
    Objects.requireNonNull(organizationId, "organizationId");
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("search text must not be blank");
    }
    language =
        (language == null || language.isBlank())
            ? null
            : language.trim().toLowerCase(java.util.Locale.ROOT);
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    } else if (limit > MAX_LIMIT) {
      limit = MAX_LIMIT;
    }
  }

  /** Backwards-compatible factory without a language filter. */
  public SearchQuery(UUID organizationId, UUID repositoryId, String text, int limit) {
    this(organizationId, repositoryId, null, text, limit);
  }

  public Optional<UUID> repositoryFilter() {
    return Optional.ofNullable(repositoryId);
  }

  public Optional<String> languageFilter() {
    return Optional.ofNullable(language);
  }
}
