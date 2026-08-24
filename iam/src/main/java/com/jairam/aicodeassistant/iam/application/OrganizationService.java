package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.event.OrganizationCreated;
import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.Organization;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.model.Role;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.port.MembershipRepository;
import com.jairam.aicodeassistant.iam.domain.port.OrganizationRepository;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import com.jairam.aicodeassistant.platform.error.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for organizations and their memberships.
 *
 * <p>Creating an organization makes the creator its {@code OWNER}. Adding a member is authorised at
 * the domain level here (the caller must hold at least {@code ADMIN} in the target organization) in
 * addition to the coarse authentication gate enforced by Spring Security at the REST edge.
 */
@Service
public class OrganizationService {

  private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final MembershipLookup membershipLookup;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public OrganizationService(
      OrganizationRepository organizations,
      MembershipRepository memberships,
      UserRepository users,
      MembershipLookup membershipLookup,
      ApplicationEventPublisher events,
      Clock clock) {
    this.organizations = organizations;
    this.memberships = memberships;
    this.users = users;
    this.membershipLookup = membershipLookup;
    this.events = events;
    this.clock = clock;
  }

  /** Creates an organization owned by {@code creatorId}. */
  @Transactional
  public CreateOrganizationResult create(java.util.UUID creatorId, String name) {
    if (name == null || name.isBlank()) {
      throw new ValidationException("Organization name must not be blank");
    }
    Instant now = clock.instant();
    Organization organization = uniqueOrganization(name, now);
    Organization saved = organizations.save(organization);

    UserId owner = new UserId(creatorId);
    memberships.save(Membership.grant(owner, saved.id(), Role.OWNER, now));

    events.publishEvent(OrganizationCreated.of(saved.id().value(), owner.value(), now));
    log.info("Created organization {} owned by {}", saved.id(), owner);
    return new CreateOrganizationResult(saved.id().value(), saved.slug());
  }

  /**
   * Adds (or re-roles) a member in an organization.
   *
   * @param actingUserId the caller, who must hold at least ADMIN in the org
   * @param organizationId target organization
   * @param targetUserId the user being added
   * @param role the role to grant
   */
  @Transactional
  public void addMember(
      java.util.UUID actingUserId, String organizationId, String targetUserId, Role role) {
    OrganizationId orgId = OrganizationId.of(organizationId);
    organizations
        .findById(orgId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));

    requireAtLeast(new UserId(actingUserId), orgId, Role.ADMIN);

    UserId target = UserId.of(targetUserId);
    User targetUser =
        users
            .findById(target)
            .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

    Instant now = clock.instant();
    memberships
        .findByUserAndOrganization(target, orgId)
        .ifPresentOrElse(
            existing -> {
              existing.changeRole(role);
              memberships.save(existing);
            },
            () -> memberships.save(Membership.grant(targetUser.id(), orgId, role, now)));
    // Invalidate any cached role (including a cached "not a member") for this user+org.
    membershipLookup.evict(target.value(), orgId.value());
    log.info("User {} set role {} for user {} in org {}", actingUserId, role, target, orgId);
  }

  private void requireAtLeast(UserId actingUserId, OrganizationId orgId, Role required) {
    Role actual =
        memberships
            .findByUserAndOrganization(actingUserId, orgId)
            .map(Membership::role)
            .orElseThrow(() -> forbidden(orgId));
    if (!actual.satisfies(required)) {
      throw forbidden(orgId);
    }
  }

  private static InsufficientRoleException forbidden(OrganizationId orgId) {
    return new InsufficientRoleException(orgId.value().toString());
  }

  private Organization uniqueOrganization(String name, Instant now) {
    Organization organization = Organization.create(name, now);
    // Slug collisions are rare; disambiguate deterministically if needed.
    if (organizations.existsBySlug(organization.slug())) {
      String disambiguated = organization.slug() + "-" + Long.toString(now.toEpochMilli(), 36);
      organization = Organization.rehydrate(organization.id(), name, disambiguated, now, now, 0L);
    }
    return organization;
  }

  /** Result of organization creation. */
  public record CreateOrganizationResult(java.util.UUID organizationId, String slug) {}

  /** Raised when the acting user lacks the required role. Maps to 403. */
  public static final class InsufficientRoleException
      extends com.jairam.aicodeassistant.platform.error.ApplicationException {
    private static final long serialVersionUID = 1L;

    InsufficientRoleException(String organizationId) {
      super(
          com.jairam.aicodeassistant.platform.error.ErrorType.AUTHORIZATION,
          org.springframework.http.HttpStatus.FORBIDDEN,
          "Insufficient role for this organization",
          Map.of("organizationId", organizationId));
    }
  }
}
