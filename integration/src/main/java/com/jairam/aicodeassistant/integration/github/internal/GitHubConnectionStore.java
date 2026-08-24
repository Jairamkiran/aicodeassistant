package com.jairam.aicodeassistant.integration.github.internal;

import java.util.Optional;
import java.util.UUID;

/** Outbound port for persisting a user's GitHub connection. Internal to the module. */
public interface GitHubConnectionStore {

  GitHubConnection save(GitHubConnection connection);

  Optional<GitHubConnection> findByUserId(UUID userId);
}
