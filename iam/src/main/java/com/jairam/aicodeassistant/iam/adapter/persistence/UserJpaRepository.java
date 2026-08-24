package com.jairam.aicodeassistant.iam.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link UserEntity}. Package-private to the adapter. */
interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);

  boolean existsByEmail(String email);
}
