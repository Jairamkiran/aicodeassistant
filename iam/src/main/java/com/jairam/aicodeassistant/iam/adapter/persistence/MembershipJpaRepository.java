package com.jairam.aicodeassistant.iam.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link MembershipEntity}. */
interface MembershipJpaRepository extends JpaRepository<MembershipEntity, UUID> {

  Optional<MembershipEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

  List<MembershipEntity> findByUserId(UUID userId);

  List<MembershipEntity> findByOrganizationId(UUID organizationId);
}
