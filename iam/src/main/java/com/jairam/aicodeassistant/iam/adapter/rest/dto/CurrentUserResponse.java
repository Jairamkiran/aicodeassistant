package com.jairam.aicodeassistant.iam.adapter.rest.dto;

import java.util.List;
import java.util.UUID;

/**
 * The authenticated user's profile plus their organization memberships.
 *
 * @param id user id
 * @param email user email
 * @param displayName user display name
 * @param status lifecycle status
 * @param memberships organizations the user belongs to, with their role
 */
public record CurrentUserResponse(
    UUID id, String email, String displayName, String status, List<MembershipView> memberships) {

  /** One membership row: which organization and what role. */
  public record MembershipView(UUID organizationId, String role) {}
}
