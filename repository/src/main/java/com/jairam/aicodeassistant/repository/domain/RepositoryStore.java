package com.jairam.aicodeassistant.repository.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and loading {@link Repository} aggregates.
 *
 * <p>Named {@code RepositoryStore} (not {@code RepositoryRepository}) to avoid the awkward stutter
 * while keeping the "repository pattern" intent clear.
 */
public interface RepositoryStore {

  Repository save(Repository repository);

  Optional<Repository> findById(RepositoryId id);

  /** Deletes a repository by id. No-op if it does not exist. */
  void deleteById(RepositoryId id);

  List<Repository> findByOrganization(UUID organizationId);

  boolean existsByOrgProviderExternalId(UUID organizationId, String provider, String externalId);
}
