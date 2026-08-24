package com.jairam.aicodeassistant.iam.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Membership aggregate — links a {@link User} to an {@link Organization} with a {@link Role}. The
 * (user, organization) pair is unique (enforced in the schema); a user therefore has at most one
 * role per organization.
 */
public final class Membership {

  private final MembershipId id;
  private final UserId userId;
  private final OrganizationId organizationId;
  private Role role;
  private final Instant createdAt;
  private final long version;

  private Membership(
      MembershipId id,
      UserId userId,
      OrganizationId organizationId,
      Role role,
      Instant createdAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.role = Objects.requireNonNull(role, "role");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.version = version;
  }

  /** Grants {@code user} the given {@code role} in {@code organization}. */
  public static Membership grant(
      UserId userId, OrganizationId organizationId, Role role, Instant now) {
    return new Membership(MembershipId.newId(), userId, organizationId, role, now, 0L);
  }

  /** Reconstructs a membership loaded from persistence. */
  public static Membership rehydrate(
      MembershipId id,
      UserId userId,
      OrganizationId organizationId,
      Role role,
      Instant createdAt,
      long version) {
    return new Membership(id, userId, organizationId, role, createdAt, version);
  }

  /** Changes the role held in the organization. */
  public void changeRole(Role newRole) {
    this.role = Objects.requireNonNull(newRole, "newRole");
  }

  public MembershipId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public Role role() {
    return role;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public long version() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof Membership other && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
