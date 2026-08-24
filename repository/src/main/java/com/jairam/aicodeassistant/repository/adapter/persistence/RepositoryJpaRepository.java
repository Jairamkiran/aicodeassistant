package com.jairam.aicodeassistant.repository.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link RepositoryEntity}. */
interface RepositoryJpaRepository extends JpaRepository<RepositoryEntity, UUID> {

  List<RepositoryEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

  boolean existsByOrganizationIdAndProviderAndExternalId(
      UUID organizationId, String provider, String externalId);
}
