package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.OrganizationId;
import com.jairam.aicodeassistant.iam.domain.model.Role;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.port.MembershipRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cacheable resolver for a user's role within an organization — the read hit that fires on
 * <em>every authorized request</em> (see {@link OrganizationAccessService}). Extracted into its own
 * bean so Spring's caching proxy applies (self-invocation would bypass it).
 *
 * <p>The {@code org-membership} cache is short-lived and small (Caffeine, configured in the app),
 * so a stale entry is bounded to seconds; membership mutations {@link #evict evict} the affected
 * entry immediately for correctness. Modelled as {@code Optional<Role>} so "not a member" is cached
 * too (avoids a repeated miss for anonymous/foreign users).
 */
@Service
public class MembershipLookup {

  static final String CACHE = "org-membership";

  private final MembershipRepository memberships;

  MembershipLookup(MembershipRepository memberships) {
    this.memberships = memberships;
  }

  /** Resolves the caller's role, caching the result (present or empty) by (user, org). */
  @Cacheable(cacheNames = CACHE, key = "#userId + ':' + #organizationId")
  @Transactional(readOnly = true)
  public Optional<Role> role(UUID userId, UUID organizationId) {
    return memberships
        .findByUserAndOrganization(new UserId(userId), new OrganizationId(organizationId))
        .map(Membership::role);
  }

  /** Evicts the cached role for a (user, org) pair after a membership change. */
  @CacheEvict(cacheNames = CACHE, key = "#userId + ':' + #organizationId")
  public void evict(UUID userId, UUID organizationId) {
    // Body intentionally empty: the annotation performs the eviction.
  }
}
