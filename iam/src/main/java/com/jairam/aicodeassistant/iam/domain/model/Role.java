package com.jairam.aicodeassistant.iam.domain.model;

/**
 * A member's role within an organization, forming a strict privilege hierarchy: {@code OWNER >
 * ADMIN > MEMBER > VIEWER}.
 *
 * <p>{@link #rank()} enables privilege comparisons ("does this role satisfy at least ADMIN?")
 * without scattering ordinal arithmetic across the codebase. Authorities exposed to Spring Security
 * are derived from {@link #authority()}.
 */
public enum Role {
  VIEWER(0),
  MEMBER(1),
  ADMIN(2),
  OWNER(3);

  private final int rank;

  Role(int rank) {
    this.rank = rank;
  }

  /** Higher rank = more privilege. */
  public int rank() {
    return rank;
  }

  /** True if this role is at least as privileged as {@code required}. */
  public boolean satisfies(Role required) {
    return this.rank >= required.rank;
  }

  /** Spring Security authority string, e.g. {@code ROLE_ADMIN}. */
  public String authority() {
    return "ROLE_" + name();
  }
}
