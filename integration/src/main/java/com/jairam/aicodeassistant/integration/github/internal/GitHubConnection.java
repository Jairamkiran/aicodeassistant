package com.jairam.aicodeassistant.integration.github.internal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A user's linked GitHub account. Holds the ENCRYPTED access token (never plaintext) plus
 * non-secret metadata. Internal to the integration module — no other context sees this type or the
 * token.
 */
public final class GitHubConnection {

  private final UUID id;
  private final UUID userId;
  private final String githubLogin;
  private final long githubUserId;
  private String accessTokenEnc;
  private String scopes;
  private final Instant connectedAt;
  private Instant updatedAt;
  private final long version;

  private GitHubConnection(
      UUID id,
      UUID userId,
      String githubLogin,
      long githubUserId,
      String accessTokenEnc,
      String scopes,
      Instant connectedAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.githubLogin = Objects.requireNonNull(githubLogin, "githubLogin");
    this.githubUserId = githubUserId;
    this.accessTokenEnc = Objects.requireNonNull(accessTokenEnc, "accessTokenEnc");
    this.scopes = scopes == null ? "" : scopes;
    this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /** First-time link. */
  public static GitHubConnection link(
      UUID userId,
      String githubLogin,
      long githubUserId,
      String accessTokenEnc,
      String scopes,
      Instant now) {
    return new GitHubConnection(
        UUID.randomUUID(), userId, githubLogin, githubUserId, accessTokenEnc, scopes, now, now, 0L);
  }

  /** Reconstruct from persistence. */
  public static GitHubConnection rehydrate(
      UUID id,
      UUID userId,
      String githubLogin,
      long githubUserId,
      String accessTokenEnc,
      String scopes,
      Instant connectedAt,
      Instant updatedAt,
      long version) {
    return new GitHubConnection(
        id,
        userId,
        githubLogin,
        githubUserId,
        accessTokenEnc,
        scopes,
        connectedAt,
        updatedAt,
        version);
  }

  /** Update the stored (encrypted) token, e.g. on re-link. */
  public void updateToken(String newAccessTokenEnc, String newScopes, Instant now) {
    this.accessTokenEnc = Objects.requireNonNull(newAccessTokenEnc, "newAccessTokenEnc");
    this.scopes = newScopes == null ? "" : newScopes;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  public UUID id() {
    return id;
  }

  public UUID userId() {
    return userId;
  }

  public String githubLogin() {
    return githubLogin;
  }

  public long githubUserId() {
    return githubUserId;
  }

  public String accessTokenEnc() {
    return accessTokenEnc;
  }

  public String scopes() {
    return scopes;
  }

  public Instant connectedAt() {
    return connectedAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }
}
