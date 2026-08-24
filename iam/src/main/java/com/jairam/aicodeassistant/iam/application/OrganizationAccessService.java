package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.api.OrganizationAccess;
import com.jairam.aicodeassistant.iam.domain.model.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Default {@link OrganizationAccess}, backed by a cached membership lookup. Owns the
 * role→capability mapping so the {@code Role} type never leaves iam.
 */
@Service
class OrganizationAccessService implements OrganizationAccess {

  private final MembershipLookup membershipLookup;

  OrganizationAccessService(MembershipLookup membershipLookup) {
    this.membershipLookup = membershipLookup;
  }

  @Override
  public boolean isMember(UUID userId, UUID organizationId) {
    return role(userId, organizationId).isPresent();
  }

  @Override
  public boolean canRead(UUID userId, UUID organizationId) {
    return hasAtLeast(userId, organizationId, Role.VIEWER);
  }

  @Override
  public boolean canContribute(UUID userId, UUID organizationId) {
    return hasAtLeast(userId, organizationId, Role.MEMBER);
  }

  @Override
  public boolean canAdminister(UUID userId, UUID organizationId) {
    return hasAtLeast(userId, organizationId, Role.ADMIN);
  }

  private boolean hasAtLeast(UUID userId, UUID organizationId, Role required) {
    return role(userId, organizationId).map(r -> r.satisfies(required)).orElse(false);
  }

  private Optional<Role> role(UUID userId, UUID organizationId) {
    return membershipLookup.role(userId, organizationId);
  }
}
