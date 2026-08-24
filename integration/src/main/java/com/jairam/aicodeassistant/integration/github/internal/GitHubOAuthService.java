package com.jairam.aicodeassistant.integration.github.internal;

import com.jairam.aicodeassistant.integration.config.GitHubProperties;
import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import com.jairam.aicodeassistant.platform.crypto.EncryptionService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drives the GitHub OAuth web flow: builds the authorize-redirect URL and handles the callback
 * (exchange code → identify user → encrypt + store token).
 *
 * <p>The access token is encrypted with {@link EncryptionService} before it touches the database
 * and is never logged. Re-linking updates the existing connection row (one per user).
 */
@Service
public class GitHubOAuthService {

  private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);
  private static final String SCOPES = "read:user,repo";

  private final GitHubApiClient client;
  private final GitHubConnectionStore connections;
  private final EncryptionService encryption;
  private final GitHubProperties properties;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public GitHubOAuthService(
      GitHubApiClient client,
      GitHubConnectionStore connections,
      EncryptionService encryption,
      GitHubProperties properties,
      ApplicationEventPublisher events,
      Clock clock) {
    this.client = client;
    this.connections = connections;
    this.encryption = encryption;
    this.properties = properties;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Builds the GitHub authorize URL to redirect the user to. {@code state} is an opaque anti-CSRF
   * value the caller binds to the session and re-checks on callback.
   */
  public String buildAuthorizeUrl(String state) {
    return properties.authBaseUrl()
        + "/authorize"
        + "?client_id="
        + enc(properties.clientId())
        + "&redirect_uri="
        + enc(properties.redirectUri())
        + "&scope="
        + enc(SCOPES)
        + "&state="
        + enc(state);
  }

  /**
   * Completes the OAuth flow for {@code userId}: exchanges the code, fetches the GitHub identity,
   * and stores the encrypted token.
   */
  @Transactional
  public void completeLink(UUID userId, String code) {
    var token =
        client.exchangeCodeForToken(
            code, properties.clientId(), properties.clientSecret(), properties.redirectUri());
    var ghUser = client.fetchAuthenticatedUser(token.accessToken());

    String encrypted = encryption.encrypt(token.accessToken());
    Instant now = clock.instant();

    connections
        .findByUserId(userId)
        .ifPresentOrElse(
            existing -> {
              existing.updateToken(encrypted, token.scopes(), now);
              connections.save(existing);
            },
            () ->
                connections.save(
                    GitHubConnection.link(
                        userId, ghUser.login(), ghUser.id(), encrypted, token.scopes(), now)));

    events.publishEvent(
        AuditSignal.builder("GITHUB_LINKED")
            .success(true)
            .actor("USER", userId.toString())
            .target("GITHUB_ACCOUNT", ghUser.login())
            .occurredAt(now)
            .build());
    log.info("Linked GitHub account {} for user {}", ghUser.login(), userId);
  }

  private static String enc(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
