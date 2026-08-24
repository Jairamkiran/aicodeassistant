package com.jairam.aicodeassistant.iam.adapter.persistence;

import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.port.MembershipRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the {@link MembershipRepository} domain port. */
@Component
class JpaMembershipRepository implements MembershipRepository {

  private final MembershipJpaRepository jpa;

  JpaMembershipRepository(MembershipJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Membership save(Membership membership) {
    return IamPersistenceMapper.toDomain(jpa.save(IamPersistenceMapper.toEntity(membership)));
  }

  @Override
  public Optional<Membership> findByUserAndOrganization(
      UserId userId, OrganizationId organizationId) {
    return jpa.findByUserIdAndOrganizationId(userId.value(), organizationId.value())
        .map(IamPersistenceMapper::toDomain);
  }

  @Override
  public List<Membership> findByUser(UserId userId) {
    return jpa.findByUserId(userId.value()).stream().map(IamPersistenceMapper::toDomain).toList();
  }

  @Override
  public List<Membership> findByOrganization(OrganizationId organizationId) {
    return jpa.findByOrganizationId(organizationId.value()).stream()
        .map(IamPersistenceMapper::toDomain)
        .toList();
  }
}
