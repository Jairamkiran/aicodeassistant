package com.jairam.aicodeassistant.integration.github.internal;

import com.jairam.aicodeassistant.integration.github.GitHubException;
import com.jairam.aicodeassistant.integration.github.GitHubGateway;
import com.jairam.aicodeassistant.integration.github.GitHubRepo;
import com.jairam.aicodeassistant.platform.crypto.EncryptionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Default {@link GitHubGateway}: resolves the caller's encrypted token, delegates to {@link
 * GitHubApiClient}, and applies resilience.
 *
 * <p>Resilience is declared here, once, via Resilience4j annotations:
 *
 * <ul>
 *   <li><b>@Retry</b> — retries only {@link GitHubException.Unavailable} (transient: timeouts, 429,
 *       5xx). Credential rejections and "not linked" are NOT retried (configured by {@code
 *       retryExceptions}/{@code ignoreExceptions} in config).
 *   <li><b>@CircuitBreaker</b> — opens after a failure-rate threshold so we stop hammering an
 *       unhealthy GitHub; a half-open probe restores traffic.
 * </ul>
 *
 * The read timeout is enforced by the RestClient's request factory (see {@code
 * GitHubIntegrationConfig}).
 */
@Service
class ResilientGitHubGateway implements GitHubGateway {

  private static final String BACKEND = "github";

  private final GitHubApiClient client;
  private final GitHubConnectionStore connections;
  private final EncryptionService encryption;

  ResilientGitHubGateway(
      GitHubApiClient client, GitHubConnectionStore connections, EncryptionService encryption) {
    this.client = client;
    this.connections = connections;
    this.encryption = encryption;
  }

  @Override
  public boolean isLinked(UUID userId) {
    return connections.findByUserId(userId).isPresent();
  }

  @Override
  @CircuitBreaker(name = BACKEND)
  @Retry(name = BACKEND)
  public List<GitHubRepo> listRepositories(UUID userId) {
    return client.listRepositories(tokenFor(userId));
  }

  @Override
  @CircuitBreaker(name = BACKEND)
  @Retry(name = BACKEND)
  public GitHubRepo getRepository(UUID userId, String owner, String name) {
    return client.getRepository(tokenFor(userId), owner, name);
  }

  /** Resolves and decrypts the user's token, or fails with NotLinked. */
  private String tokenFor(UUID userId) {
    GitHubConnection connection =
        connections.findByUserId(userId).orElseThrow(GitHubException.NotLinked::new);
    return encryption.decrypt(connection.accessTokenEnc());
  }
}
