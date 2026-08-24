package com.jairam.aicodeassistant.integration.github.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed {@link GitHubConnectionStore}. */
@Component
class JpaGitHubConnectionStore implements GitHubConnectionStore {

  private final GitHubConnectionJpaRepository jpa;

  JpaGitHubConnectionStore(GitHubConnectionJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public GitHubConnection save(GitHubConnection c) {
    GitHubConnectionEntity saved =
        jpa.save(
            new GitHubConnectionEntity(
                c.id(),
                c.userId(),
                c.githubLogin(),
                c.githubUserId(),
                c.accessTokenEnc(),
                c.scopes(),
                c.connectedAt(),
                c.updatedAt(),
                c.version()));
    return toDomain(saved);
  }

  @Override
  public Optional<GitHubConnection> findByUserId(UUID userId) {
    return jpa.findByUserId(userId).map(JpaGitHubConnectionStore::toDomain);
  }

  private static GitHubConnection toDomain(GitHubConnectionEntity e) {
    return GitHubConnection.rehydrate(
        e.getId(),
        e.getUserId(),
        e.getGithubLogin(),
        e.getGithubUserId(),
        e.getAccessTokenEnc(),
        e.getScopes(),
        e.getConnectedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }
}
