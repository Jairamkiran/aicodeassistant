package com.jairam.aicodeassistant.iam.domain.apikey;

import com.jairam.aicodeassistant.iam.domain.model.UserId;
import java.util.List;
import java.util.Optional;

/** Outbound port for API-key persistence. */
public interface ApiKeyStore {

  ApiKey save(ApiKey apiKey);

  Optional<ApiKey> findById(ApiKeyId id);

  /** Looks up an active key by its non-secret prefix (the auth hot path). */
  Optional<ApiKey> findActiveByPrefix(String keyPrefix);

  List<ApiKey> findByUser(UserId userId);
}
