package com.jairam.aicodeassistant.iam.adapter.persistence;

import com.jairam.aicodeassistant.iam.domain.apikey.ApiKey;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyId;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyStatus;
import com.jairam.aicodeassistant.iam.domain.apikey.ApiKeyStore;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the {@link ApiKeyStore} domain port. */
@Component
class JpaApiKeyStore implements ApiKeyStore {

  private final ApiKeyJpaRepository jpa;

  JpaApiKeyStore(ApiKeyJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public ApiKey save(ApiKey apiKey) {
    return IamPersistenceMapper.toDomain(jpa.save(IamPersistenceMapper.toEntity(apiKey)));
  }

  @Override
  public Optional<ApiKey> findById(ApiKeyId id) {
    return jpa.findById(id.value()).map(IamPersistenceMapper::toDomain);
  }

  @Override
  public Optional<ApiKey> findActiveByPrefix(String keyPrefix) {
    return jpa.findByKeyPrefixAndStatus(keyPrefix, ApiKeyStatus.ACTIVE.name())
        .map(IamPersistenceMapper::toDomain);
  }

  @Override
  public List<ApiKey> findByUser(UserId userId) {
    return jpa.findByUserIdOrderByCreatedAtDesc(userId.value()).stream()
        .map(IamPersistenceMapper::toDomain)
        .toList();
  }
}
