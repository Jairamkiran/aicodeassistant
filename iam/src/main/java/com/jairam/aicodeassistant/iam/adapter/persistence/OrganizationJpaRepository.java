package com.jairam.aicodeassistant.iam.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link OrganizationEntity}. */
interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, UUID> {

  boolean existsBySlug(String slug);
}
