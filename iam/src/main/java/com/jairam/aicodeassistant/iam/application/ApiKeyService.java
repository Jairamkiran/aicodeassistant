package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.apikey.ApiKey;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyGenerator;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyId;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyScope;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyStore;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Management use cases for API keys: issue, list, revoke.
 *
 * <p>Issuing returns the raw key <em>once</em> (in {@link IssueResult}); it is never retrievable
 * again, matching how every credible SaaS handles API keys. Lifecycle changes publish application
 * events for the audit log.
 */
@Service
public class ApiKeyService {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

  private final ApiKeyStore store;
  private final ApiKeyGenerator generator;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public ApiKeyService(
      ApiKeyStore store, ApiKeyGenerator generator, ApplicationEventPublisher events, Clock clock) {
    this.store = store;
    this.generator = generator;
    this.events = events;
    this.clock = clock;
  }

  /** Issues a new key for {@code ownerId}. {@code ttl} null = no expiry. */
  @Transactional
  public IssueResult issue(UUID ownerId, String name, Set<ApiKeyScope> scopes, Duration ttl) {
    Instant now = clock.instant();
    Instant expiresAt = ttl == null ? null : now.plus(ttl);
    ApiKeyGenerator.GeneratedApiKey generated = generator.generate();

    ApiKey key =
        ApiKey.issue(
            new UserId(ownerId),
            name,
            generated.keyPrefix(),
            generated.secretHash(),
            scopes,
            now,
            expiresAt);
    ApiKey saved = store.save(key);

    events.publishEvent(
        AuditSignal.builder("API_KEY_CREATED")
            .success(true)
            .actor("USER", ownerId.toString())
            .target("API_KEY", saved.id().toString())
            .occurredAt(now)
            .build());
    log.info("Issued API key {} (prefix {}) for user {}", saved.id(), saved.keyPrefix(), ownerId);

    // The raw key is returned exactly once — never persisted or logged.
    return new IssueResult(saved.id().value(), generated.rawKey(), saved.keyPrefix(), expiresAt);
  }

  /** Lists the caller's keys (metadata only — never secrets). */
  @Transactional(readOnly = true)
  public List<ApiKey> list(UUID ownerId) {
    return store.findByUser(new UserId(ownerId));
  }

  /** Revokes a key the caller owns. */
  @Transactional
  public void revoke(UUID ownerId, String apiKeyId) {
    ApiKey key =
        store
            .findById(ApiKeyId.of(apiKeyId))
            .filter(k -> k.userId().value().equals(ownerId))
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", apiKeyId));

    Instant now = clock.instant();
    key.revoke(now);
    store.save(key);
    events.publishEvent(
        AuditSignal.builder("API_KEY_REVOKED")
            .success(true)
            .actor("USER", ownerId.toString())
            .target("API_KEY", key.id().toString())
            .occurredAt(now)
            .build());
    log.info("Revoked API key {} for user {}", key.id(), ownerId);
  }

  /** One-time issuance result carrying the raw key. */
  public record IssueResult(UUID apiKeyId, String rawKey, String keyPrefix, Instant expiresAt) {}
}
