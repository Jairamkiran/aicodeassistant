package com.jairam.aicodeassistant.repository.adapter.persistence;

import com.jairam.aicodeassistant.repository.domain.ImportStatus;
import com.jairam.aicodeassistant.repository.domain.Repository;
import com.jairam.aicodeassistant.repository.domain.RepositoryId;
import com.jairam.aicodeassistant.repository.domain.RepositoryStore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the {@link RepositoryStore} domain port. */
@Component
class JpaRepositoryStore implements RepositoryStore {

  private final RepositoryJpaRepository jpa;

  JpaRepositoryStore(RepositoryJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Repository save(Repository repository) {
    return toDomain(jpa.save(toEntity(repository)));
  }

  @Override
  public Optional<Repository> findById(RepositoryId id) {
    return jpa.findById(id.value()).map(JpaRepositoryStore::toDomain);
  }

  @Override
  public void deleteById(RepositoryId id) {
    jpa.deleteById(id.value());
  }

  @Override
  public List<Repository> findByOrganization(UUID organizationId) {
    return jpa.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
        .map(JpaRepositoryStore::toDomain)
        .toList();
  }

  @Override
  public boolean existsByOrgProviderExternalId(
      UUID organizationId, String provider, String externalId) {
    return jpa.existsByOrganizationIdAndProviderAndExternalId(organizationId, provider, externalId);
  }

  private static RepositoryEntity toEntity(Repository r) {
    return new RepositoryEntity(
        r.id().value(),
        r.organizationId(),
        r.provider(),
        r.externalId(),
        r.owner(),
        r.name(),
        r.cloneUrl(),
        r.defaultBranch(),
        r.isPrivate(),
        r.status().name(),
        r.registeredBy(),
        r.createdAt(),
        r.updatedAt(),
        r.statusDetail(),
        r.version());
  }

  private static Repository toDomain(RepositoryEntity e) {
    return Repository.rehydrate(
        new RepositoryId(e.getId()),
        e.getOrganizationId(),
        e.getProvider(),
        e.getExternalId(),
        e.getOwner(),
        e.getName(),
        e.getCloneUrl(),
        e.getDefaultBranch(),
        e.isPrivate(),
        ImportStatus.valueOf(e.getStatus()),
        e.getRegisteredBy(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getStatusDetail(),
        e.getVersion());
  }
}
