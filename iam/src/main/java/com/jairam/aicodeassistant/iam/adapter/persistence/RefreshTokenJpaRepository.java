package com.jairam.aicodeassistant.iam.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link RefreshTokenEntity}. */
interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  /**
   * Bulk-revokes every still-active token in a family (reuse-detection response). Returns the
   * number of rows updated. {@code clearAutomatically}/{@code flushAutomatically} keep the
   * persistence context consistent with the bulk update.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update RefreshTokenEntity t set t.revokedAt = :now "
          + "where t.familyId = :familyId and t.revokedAt is null")
  int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

  /** Deletes tokens whose expiry is before the cutoff. Returns the number of rows removed. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  int deleteByExpiresAtBefore(Instant cutoff);
}
