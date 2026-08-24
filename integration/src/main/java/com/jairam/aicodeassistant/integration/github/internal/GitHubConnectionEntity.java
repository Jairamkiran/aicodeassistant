package com.jairam.aicodeassistant.integration.github.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** JPA entity for {@code github_connections}. The token column holds ciphertext only. */
@Entity
@Table(name = "github_connections")
class GitHubConnectionEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "github_login", nullable = false)
  private String githubLogin;

  @Column(name = "github_user_id", nullable = false)
  private long githubUserId;

  @Column(name = "access_token_enc", nullable = false)
  private String accessTokenEnc;

  @Column(nullable = false)
  private String scopes;

  @Column(name = "connected_at", nullable = false, updatable = false)
  private Instant connectedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected GitHubConnectionEntity() {
    // Required by JPA.
  }

  GitHubConnectionEntity(
      UUID id,
      UUID userId,
      String githubLogin,
      long githubUserId,
      String accessTokenEnc,
      String scopes,
      Instant connectedAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.userId = userId;
    this.githubLogin = githubLogin;
    this.githubUserId = githubUserId;
    this.accessTokenEnc = accessTokenEnc;
    this.scopes = scopes;
    this.connectedAt = connectedAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  UUID getId() {
    return id;
  }

  UUID getUserId() {
    return userId;
  }

  String getGithubLogin() {
    return githubLogin;
  }

  long getGithubUserId() {
    return githubUserId;
  }

  String getAccessTokenEnc() {
    return accessTokenEnc;
  }

  String getScopes() {
    return scopes;
  }

  Instant getConnectedAt() {
    return connectedAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  long getVersion() {
    return version;
  }
}
