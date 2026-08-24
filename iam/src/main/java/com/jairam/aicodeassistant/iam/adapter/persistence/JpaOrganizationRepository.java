package com.jairam.aicodeassistant.iam.adapter.persistence;

import com.jairam.aicodeassistant.iam.domain.model.Organization;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.port.OrganizationRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the {@link OrganizationRepository} domain port. */
@Component
class JpaOrganizationRepository implements OrganizationRepository {

  private final OrganizationJpaRepository jpa;

  JpaOrganizationRepository(OrganizationJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Organization save(Organization organization) {
    return IamPersistenceMapper.toDomain(jpa.save(IamPersistenceMapper.toEntity(organization)));
  }

  @Override
  public Optional<Organization> findById(OrganizationId id) {
    return jpa.findById(id.value()).map(IamPersistenceMapper::toDomain);
  }

  @Override
  public boolean existsBySlug(String slug) {
    return jpa.existsBySlug(slug);
  }
}
