package com.jairam.aicodeassistant.iam.api;

import java.util.UUID;

/**
 * Public API other modules use to authorize organization-scoped actions without reaching into iam
 * internals or learning the {@code Role} type.
 *
 * <p>Intent-based ("tell, don't ask"): callers ask whether a user may perform a class of action,
 * and iam owns the role→capability mapping. This keeps the role model entirely inside iam — a
 * second context never switches on roles itself.
 *
 * <p>Exposed because a real consumer exists (the repository context authorizes imports) — not added
 * speculatively.
 */
public interface OrganizationAccess {

  /** True if the user is a member of the organization in any role. */
  boolean isMember(UUID userId, UUID organizationId);

  /** True if the user may read organization-scoped resources (VIEWER+). */
  boolean canRead(UUID userId, UUID organizationId);

  /** True if the user may create/modify org resources such as importing repos (MEMBER+). */
  boolean canContribute(UUID userId, UUID organizationId);

  /** True if the user may administer the organization (ADMIN+). */
  boolean canAdminister(UUID userId, UUID organizationId);
}
