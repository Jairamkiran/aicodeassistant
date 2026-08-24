package com.jairam.aicodeassistant.integration.github.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link GitHubConnectionEntity}. */
interface GitHubConnectionJpaRepository extends JpaRepository<GitHubConnectionEntity, UUID> {

  Optional<GitHubConnectionEntity> findByUserId(UUID userId);
}
