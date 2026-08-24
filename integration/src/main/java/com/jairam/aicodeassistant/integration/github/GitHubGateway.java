package com.jairam.aicodeassistant.integration.github;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the GitHub integration — the ONLY GitHub surface other modules (e.g. repository)
 * may use. Everything provider-specific (HTTP, JSON DTOs, OAuth, the encrypted token) is
 * encapsulated behind this interface.
 *
 * <p>The token for a user is resolved internally by userId, so callers never handle credentials.
 * Failures are reported as {@link GitHubException} subtypes, not provider HTTP errors.
 */
public interface GitHubGateway {

  /** Whether the user has a usable linked GitHub connection. */
  boolean isLinked(UUID userId);

  /**
   * Lists repositories the linked user can access.
   *
   * @throws GitHubException.NotLinked if the user has not linked GitHub
   * @throws GitHubException.Unavailable if GitHub is unreachable/rate-limited/5xx
   * @throws GitHubException.CredentialRejected if the stored token is rejected
   */
  List<GitHubRepo> listRepositories(UUID userId);

  /**
   * Fetches a single repository by {@code owner/name} for the linked user.
   *
   * @throws GitHubException.NotLinked / Unavailable / CredentialRejected as above
   */
  GitHubRepo getRepository(UUID userId, String owner, String name);
}
