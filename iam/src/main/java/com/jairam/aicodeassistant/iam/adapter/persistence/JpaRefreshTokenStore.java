package com.jairam.aicodeassistant.iam.adapter.persistence;

import com.jairam.aicodeassistant.iam.domain.model.RefreshToken;
import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenStore;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the {@link RefreshTokenStore} domain port. */
@Component
class JpaRefreshTokenStore implements RefreshTokenStore {

  private final RefreshTokenJpaRepository jpa;

  JpaRefreshTokenStore(RefreshTokenJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public RefreshToken save(RefreshToken token) {
    return IamPersistenceMapper.toDomain(jpa.save(IamPersistenceMapper.toEntity(token)));
  }

  @Override
  public Optional<RefreshToken> findByTokenHash(String tokenHash) {
    return jpa.findByTokenHash(tokenHash).map(IamPersistenceMapper::toDomain);
  }

  @Override
  public int revokeFamily(UUID familyId, Instant now) {
    return jpa.revokeFamily(familyId, now);
  }

  @Override
  public int deleteExpiredBefore(Instant cutoff) {
    return jpa.deleteByExpiresAtBefore(cutoff);
  }
}
