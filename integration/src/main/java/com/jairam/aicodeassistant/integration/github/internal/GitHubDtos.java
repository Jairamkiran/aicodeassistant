package com.jairam.aicodeassistant.integration.github.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub REST/OAuth JSON payloads. These provider-shaped DTOs are package-private and MUST NOT leak
 * outside the adapter — the client converts them to the module's public {@code GitHubRepo} / domain
 * types before returning.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} keeps us resilient to GitHub adding
 * fields.
 */
final class GitHubDtos {

  private GitHubDtos() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("scope") String scope,
      @JsonProperty("error") String error,
      @JsonProperty("error_description") String errorDescription) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record UserResponse(@JsonProperty("id") long id, @JsonProperty("login") String login) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RepoResponse(
      @JsonProperty("id") long id,
      @JsonProperty("name") String name,
      @JsonProperty("full_name") String fullName,
      @JsonProperty("private") boolean isPrivate,
      @JsonProperty("clone_url") String cloneUrl,
      @JsonProperty("default_branch") String defaultBranch,
      @JsonProperty("owner") Owner owner) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Owner(@JsonProperty("login") String login) {}
  }
}
