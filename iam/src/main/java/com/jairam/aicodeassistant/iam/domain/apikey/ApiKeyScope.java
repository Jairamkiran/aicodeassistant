package com.jairam.aicodeassistant.iam.domain.apikey;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Coarse-grained permission scopes an API key may carry. Kept intentionally small for M2 — finer
 * scopes are added as the API surface grows (feature milestones). Persisted as a comma-separated
 * string of {@link #name()} values.
 */
public enum ApiKeyScope {
  /** Read repositories, chat history, analysis results. */
  READ,
  /** Trigger writes: imports, generation, reviews. */
  WRITE,
  /** Administrative actions within the owner's organizations. */
  ADMIN;

  /** Parses a comma-separated scope string, ignoring blanks/unknowns defensively. */
  public static Set<ApiKeyScope> parse(String csv) {
    Set<ApiKeyScope> scopes = new LinkedHashSet<>();
    if (csv == null || csv.isBlank()) {
      return scopes;
    }
    for (String token : csv.split(",")) {
      String trimmed = token.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      for (ApiKeyScope scope : values()) {
        if (scope.name().equalsIgnoreCase(trimmed)) {
          scopes.add(scope);
        }
      }
    }
    return scopes;
  }

  /** Serialises a set of scopes to the stored comma-separated form. */
  public static String toCsv(Set<ApiKeyScope> scopes) {
    return String.join(",", scopes.stream().map(Enum::name).toArray(String[]::new));
  }

  /** Convenience for constructing a scope set. */
  public static Set<ApiKeyScope> setOf(ApiKeyScope... scopes) {
    return new LinkedHashSet<>(Arrays.asList(scopes));
  }
}
