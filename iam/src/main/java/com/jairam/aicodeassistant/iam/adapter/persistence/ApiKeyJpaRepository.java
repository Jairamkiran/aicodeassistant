package com.jairam.aicodeassistant.iam.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ApiKeyEntity}. */
interface ApiKeyJpaRepository extends JpaRepository<ApiKeyEntity, UUID> {

  Optional<ApiKeyEntity> findByKeyPrefixAndStatus(String keyPrefix, String status);

  List<ApiKeyEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
