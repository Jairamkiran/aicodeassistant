package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.apikey.ApiKey;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyGenerator;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyScope;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyStore;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates a presented raw API key.
 *
 * <p>Flow: parse → look up the active key by its (non-secret) prefix → verify the secret hash →
 * check usability (active + not expired) → record last-use. Returns an {@link AuthenticatedApiKey}
 * on success, or empty on any failure. Returning {@code Optional.empty()} rather than throwing lets
 * the servlet filter decide how to respond and keeps this method side-effect-free on the failure
 * path (except the deliberate last-use bump on success).
 */
@Service
public class ApiKeyAuthenticator {

  private final ApiKeyStore store;
  private final ApiKeyGenerator generator;
  private final Clock clock;

  public ApiKeyAuthenticator(ApiKeyStore store, ApiKeyGenerator generator, Clock clock) {
    this.store = store;
    this.generator = generator;
    this.clock = clock;
  }

  /** Attempts to authenticate a raw API key; empty if invalid/expired/revoked. */
  @Transactional
  public Optional<AuthenticatedApiKey> authenticate(String rawKey) {
    Optional<ApiKeyGenerator.ParsedApiKey> parsed = generator.parse(rawKey);
    if (parsed.isEmpty()) {
      return Optional.empty();
    }

    Optional<ApiKey> maybeKey = store.findActiveByPrefix(parsed.get().keyPrefix());
    if (maybeKey.isEmpty()) {
      return Optional.empty();
    }

    ApiKey key = maybeKey.get();
    var now = clock.instant();
    if (!key.isUsable(now) || !generator.matches(parsed.get().secret(), key.secretHash())) {
      return Optional.empty();
    }

    key.markUsed(now);
    store.save(key);
    return Optional.of(
        new AuthenticatedApiKey(key.id().value(), key.userId().value(), key.scopes()));
  }

  /** The authenticated principal derived from a valid API key. */
  public record AuthenticatedApiKey(UUID apiKeyId, UUID userId, Set<ApiKeyScope> scopes) {}
}
